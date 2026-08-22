"use client";

import { useCallback, useMemo } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/shared/spinner";
import type { DataObject } from "@/lib/dynamic/path";
import { isHandBuilt } from "@/lib/hand-built-categories";
import { fetchDynamic } from "@/lib/rs/client";
import { DynamicActionGroup } from "./dynamic-action-group";
import { DynamicLayoutProvider } from "./dynamic-context";
import { DynamicRenderer } from "./dynamic-renderer";

export interface DynamicFormDialogProps {
  /** REST category whose `{category}/edit` layout is rendered, e.g. `group`. */
  category: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /**
   * Values written over the backend's preset, e.g. `{ name: "Project X-pm" }`.
   *
   * Compared by content, so a caller may build the object inline: a new object of the same values
   * must not re-seed the form and throw away what the user has typed.
   */
  prefill?: DataObject;
  /** The entry was created: `id` from the answer's variables, `data` the values that were saved. */
  onSaved: (id: number, data: DataObject) => void;
}

/**
 * Adds an entry of a server-laid-out entity in a dialog — the same form its own page would show.
 *
 * For a caller that needs something created *without leaving what it is doing*: the structure wizard
 * needs a group, and sending the user to the group page would abandon the wizard's other choices
 * (see WizardGroupStepCard). The form itself is the backend's `UILayout` including its buttons, so
 * nothing here knows what a group is — every field, every rule and both actions are the ones of
 * `GroupPagesRest`.
 *
 * What makes this possible is that the backend answers a save with the new id
 * (`AbstractEntityRest.onAfterEdit` puts it into the response's variables) and that a redirect has no
 * meaning inside a dialog — see [DynamicDoneHandler] for how the two are read.
 *
 * Only for a category whose form *is* a `UILayout`: a hand built page (`HAND_BUILT_CATEGORIES`) has
 * no `{category}/edit` layout to render, so the dialog says so instead of showing an empty frame.
 */
export function DynamicFormDialog({
  category,
  open,
  onOpenChange,
  prefill,
  onSaved,
}: DynamicFormDialogProps) {
  const t = useTranslations();
  const handBuilt = isHandBuilt(category);
  const queryKey = useMemo(
    () => ["dynamic", category, "edit", "dialog"] as const,
    [category]
  );

  const {
    data: response,
    isLoading,
    isError,
  } = useQuery({
    queryKey,
    queryFn: ({ signal }) => fetchDynamic(category, "edit", undefined, signal),
    // Only while it is open, and freshly every time: what the preset is depends on the moment
    // (a group's ldap defaults are read per request), and a form filled in once must not come back.
    enabled: open && !handBuilt,
    gcTime: 0,
  });

  // The values themselves rather than their object identity decide whether the form is re-seeded: a
  // caller may build the prefill inline, and a new object of the same values must not throw away what
  // the user has typed. Serialized once, so the string is both the comparison and the value.
  const prefillKey = JSON.stringify(prefill ?? null);
  const seed = useMemo(() => {
    if (!response) return undefined;
    const values = JSON.parse(prefillKey) as DataObject | null;
    return values
      ? { ...response, data: { ...response.data, ...values } }
      : response;
  }, [response, prefillKey]);

  const handleDone = useCallback(
    (variables: DataObject, data: DataObject) => {
      onOpenChange(false);
      // No id means the edit ended without a write — the cancel button, whose redirect looks the
      // same as a save's (both go through `AbstractEntityRest`).
      const id = variables.id;
      if (typeof id === "number" && id > 0) onSaved(id, data);
    },
    [onOpenChange, onSaved]
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] w-[95vw] gap-0 overflow-hidden !max-w-3xl p-0">
        <DialogHeader className="border-b px-6 py-4">
          {/* The backend's own title ("Neue Gruppe"), so the dialog is named the way the page is. */}
          <DialogTitle>{response?.ui.title ?? t("loading")}</DialogTitle>
        </DialogHeader>
        {isLoading && (
          <div className="flex items-center justify-center p-10">
            <Spinner />
          </div>
        )}
        {(isError || handBuilt) && (
          <div className="p-6 text-sm text-muted-foreground">
            {t("validation.error.generic")}
          </div>
        )}
        {seed && (
          <DynamicLayoutProvider
            response={seed}
            category={category}
            queryKey={queryKey}
            onDone={handleDone}
          >
            <div className="max-h-[60vh] overflow-auto px-6 py-4">
              <div className="flex flex-col gap-4">
                <DynamicRenderer content={seed.ui.layout} />
              </div>
            </div>
            {/* The backend's buttons are the dialog's buttons — Abbrechen and Anlegen for a group. */}
            <DynamicActionGroup />
          </DynamicLayoutProvider>
        )}
      </DialogContent>
    </Dialog>
  );
}
