"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useForm, useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import type { EntityWriteResult } from "@/lib/rs/entity";
import type { ResponseAction } from "@/lib/rs/types";
import type { EntityForm } from "@/components/shared/form/form-context";
import { applyServerValidationErrors } from "@/lib/validation/server-errors";
import { showResponseMessage } from "@/lib/dynamic/response-toast";
import { SAVE_META, type SubmitMeta } from "@/lib/rs/submit-meta";
import type { ZodType } from "zod";

/**
 * Picks the mutation a submit runs. `save` is the default; an entity with further writes (a book's
 * lend-out) returns its own mutation for its own action name.
 */
type MutateFn<Values> = (
  values: Values,
  meta: SubmitMeta
) => Promise<EntityWriteResult>;

export interface UseEntityEditFormOptions<Values, Data> {
  /** The entity as the server delivered it, or null/undefined while adding a new one. */
  data: Data | null | undefined;
  /**
   * The DTO as the form holds it. A separate step because the two shapes differ: a field Spring
   * omitted (`JsonInclude.Include.NON_NULL`) has to become null, and a value the form edits
   * differently (a comma-joined tag list) is converted here.
   *
   * Must be a stable function — it is a dependency of the reset below, so one rebuilt per render
   * would reset the form on every render and discard what the user typed.
   */
  toFormValues: (data: Data) => Values;
  /** Values of a new, empty entry. */
  defaultValues: Values;
  /** Zod schema of the form, built from the generated metadata (lib/validation/from-metadata.ts). */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  schema: ZodType<any, any>;
  /**
   * Names of the fields the form renders, so a server error naming anything else is surfaced as a
   * toast instead of being written into a field that never shows it.
   */
  fieldNames: readonly string[];
  /**
   * Names of the array (collection) fields. A bare error on one of these has no `<form.Field>` to
   * display it and must surface as a toast (see applyServerValidationErrors).
   */
  arrayFieldNames?: readonly string[];
  /** Where cancel and a successful save go, e.g. `/book`. */
  listRoute: string;
  /**
   * Where a successful save goes when the id of the written entry belongs into that url — the caller
   * that sent the user here to *create* something and goes on with it (see EditReturn.savedRoute).
   *
   * Left out by everybody else, and then [listRoute] is the way back, as it always was.
   */
  savedRoute?: (id: number | null) => string;
  /**
   * Takes the place of leaving for [listRoute] after a successful save — for a form that has no page
   * to leave: the one in a dialog ([EntityEditModal]), whose caller closes it and goes on with what
   * was written.
   *
   * Gets the id the backend assigned (null if it named none) and the values that were saved, since
   * the answer of a write carries no entity (see lib/rs/entity.ts) and a caller usually wants a name
   * as well as an id. The write's `ResponseAction` is passed along too — the calendar reads its
   * `?gotoDate=…&hash=…` redirect url to jump to the saved entry's period (see CalendarEditRouteClient).
   */
  onSaved?: (id: number | null, values: Values, action: ResponseAction) => void;
  /** Toast text of a successful save, e.g. `t("saved")`. */
  savedMessage: string;
  save: MutateFn<Values>;
}

export interface EntityEditForm {
  form: EntityForm;
  isDirty: boolean;
  isSubmitting: boolean;
}

/**
 * The submit of a hand-built edit form: Zod validation, the server's HTTP 406 answer, the success
 * toast and the way back to the list.
 *
 * The server is the authority on validation — the schema only anticipates its rules for faster
 * feedback (see lib/validation/server-errors.ts), which is why a 406 is a regular answer here and
 * not an error.
 */
export function useEntityEditForm<Values, Data>({
  data,
  toFormValues,
  defaultValues,
  schema,
  fieldNames,
  arrayFieldNames,
  listRoute,
  savedRoute,
  onSaved,
  savedMessage,
  save,
}: UseEntityEditFormOptions<Values, Data>): EntityEditForm {
  const router = useRouter();
  const tCommon = useTranslations();
  /**
   * Set by a submit whose answer has to win over what is on screen — a further action, whose
   * recomputed values are the whole point of pressing it (see below and the reset effect).
   */
  const forceReset = useRef(false);

  const form = useForm({
    defaultValues: data ? toFormValues(data) : defaultValues,
    validators: { onSubmit: schema },
    // Saving is what the form's own submit button does; further actions pass their own meta.
    onSubmitMeta: SAVE_META,
    onSubmit: async ({ value, meta }) => {
      const result = await save(value as Values, meta);
      // Before every branch below, and for every outcome: a button that acts on the write has to hear
      // about a refusal as well, and that is the case it must not act on (see SubmitMeta.onWritten).
      meta.onWritten?.(result);
      if (result.kind === "rejected") {
        // The backend refused the write and said why (an AccessException, see lib/rs/entity.ts). Not a
        // field error and not the form's own doing, so it is shown as it came and the form stays put
        // with the user's values — leaving the page would look like the save had gone through.
        toast.error(result.message || tCommon("validation.error.generic"));
        return;
      }
      if (result.kind === "validationErrors") {
        // The server rejected the entity: its rules are the authority, ours only anticipate them.
        const { unassigned, hasAssigned } = applyServerValidationErrors(
          form,
          result.validationErrors,
          fieldNames,
          arrayFieldNames
        );
        // Anything the form can't show next to a field would be invisible otherwise.
        unassigned.forEach((message) => toast.error(message));
        if (unassigned.length === 0 && !hasAssigned)
          toast.error(tCommon("validation.error.generic"));
        return;
      }
      // Something the write has to say beyond having gone through — the order's notification mail that
      // could not be sent is the case (OrderEntityRest.onAfterEdit). Shown next to the success message
      // and not instead of it: the entity *was* written, and only the extra step failed. After that
      // message, so it ends up on top of it and not behind it; it is also the only one of the two that
      // stays until it is closed (see showResponseMessage).
      const extraMessage = result.action.message;
      if (meta.action !== "save") {
        // A further action stays on the page: its result is what the user came for. The backend's
        // ResponseAction is a REDIRECT to the list here too (both run through saveOrUpdate), and is
        // ignored just as it is for a save. The values it computed arrive with the invalidated
        // detail query, and the effect below resets the form onto them — which is the one case in
        // which that reset has to overrule a dirty form, hence the flag.
        forceReset.current = true;
        toast.success(tCommon("message.successfullChanged"));
        if (extraMessage) showResponseMessage(extraMessage);
        return;
      }
      toast.success(savedMessage);
      if (extraMessage) showResponseMessage(extraMessage);
      // Back to the list, which is where the backend points too (its ResponseAction is a REDIRECT)
      // and what deleting does. The form is reset first so leaving it doesn't look like unsaved
      // changes; the list refetches on its own, the caches having been invalidated.
      form.reset(value);
      if (onSaved) {
        // A form without a page of its own: its caller decides what "afterwards" means, and there is
        // nothing to navigate to (see the option).
        onSaved(result.id, value as Values, result.action);
        return;
      }
      // The id of what was just written, for a caller that asked to be told (`savedRoute`): an insert
      // is the case that needs it, and the id only exists after this save.
      router.push(savedRoute ? savedRoute(result.id) : listRoute);
    },
  });

  // Takes the form onto the server's values — for the first load, and again whenever the entity is
  // refetched (after a further action, whose result is the point of pressing it).
  //
  // Guarded by the dirty state, because this effect runs more often than the data changes: the form
  // is hidden rather than unmounted while a side tab is open (see EditPageShell), and React re-runs
  // every effect of a hidden tree on the way back to visible. Unguarded, coming back from the
  // history tab would reset the form and throw away what the user had entered — the very bug the
  // one-route model is here to fix. What the user typed wins; only `forceReset` above overrules it.
  useEffect(() => {
    if (!data) return;
    if (form.state.isDirty && !forceReset.current) return;
    forceReset.current = false;
    form.reset(toFormValues(data));
  }, [data, toFormValues, form]);

  const isDirty = useStore(form.store, (s) => s.isDirty);
  const isSubmitting = useStore(form.store, (s) => s.isSubmitting);

  return { form, isDirty, isSubmitting };
}
