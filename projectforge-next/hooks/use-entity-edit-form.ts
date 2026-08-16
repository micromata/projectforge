"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm, useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import type { EntityWriteResult } from "@/lib/rs/entity";
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
  savedMessage,
  save,
}: UseEntityEditFormOptions<Values, Data>): EntityEditForm {
  const router = useRouter();
  const tCommon = useTranslations();

  const form = useForm({
    defaultValues: data ? toFormValues(data) : defaultValues,
    validators: { onSubmit: schema },
    // Saving is what the form's own submit button does; further actions pass their own meta.
    onSubmitMeta: SAVE_META,
    onSubmit: async ({ value, meta }) => {
      const result = await save(value as Values, meta);
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
        // detail query, and the effect below resets the form onto them.
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
      router.push(listRoute);
    },
  });

  useEffect(() => {
    if (data) form.reset(toFormValues(data));
  }, [data, toFormValues, form]);

  const isDirty = useStore(form.store, (s) => s.isDirty);
  const isSubmitting = useStore(form.store, (s) => s.isSubmitting);

  return { form, isDirty, isSubmitting };
}
