"use client";

import { useState } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CheckmarkCircle02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { entityAccess } from "@/lib/rs/entity-access";
import { TO_DO_METADATA } from "@/lib/metadata/to-do.generated";
import type { ToDoDetail } from "./types";

/**
 * Right of the save button: whether the save notifies assignee and reporter by e-mail, and the close
 * button — both of the Wicket edit form.
 *
 * The checkbox only where mail can be sent at all (`ToDo.mailConfigured`); the backend notifies on a new
 * to-do and a changed assignee, status or deletion state whatever it says (`ToDoEntityRest`).
 */
export function ToDoSaveOption() {
  const t = useTranslations();
  const data = useEntityData<ToDoDetail>();
  return (
    <div className="flex flex-wrap items-center gap-3">
      {data?.mailConfigured && (
        <CheckboxField
          name="sendNotification"
          label={t("label.sendEMailNotification")}
          hint={t("plugins.todo.notification.tooltip")}
        />
      )}
      <CloseToDoButton />
    </div>
  );
}

/**
 * Closes the to-do: a dialog edits its comment, and the confirm submits the form to the close action,
 * which saves it with the status closed (`ToDoEntityRest.close`). Shown where Wicket showed it: on a
 * stored to-do that isn't closed or deleted yet, for a user who may change it.
 */
function CloseToDoButton() {
  const t = useTranslations();
  const label = useFieldLabels(TO_DO_METADATA);
  const form = useEntityEditForm();
  const data = useEntityData<ToDoDetail>();
  const [open, setOpen] = useState(false);
  const isSubmitting = useStore(
    form.store,
    (s: unknown) => (s as { isSubmitting: boolean }).isSubmitting
  );

  const id = data?.id ?? null;
  if (id == null || data?.status === "CLOSED") return null;
  if (!entityAccess(data, false).write) return null;

  const close = async () => {
    await form.handleSubmit({ action: "close" });
    setOpen(false);
  };

  return (
    <>
      <Button
        type="button"
        variant="outline"
        size="sm"
        className="gap-1.5"
        disabled={isSubmitting}
        onClick={() => setOpen(true)}
      >
        <HugeiconsIcon icon={CheckmarkCircle02Icon} size={13} />
        {t("plugins.todo.button.close")}
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("plugins.todo.closeDialog.heading")}</DialogTitle>
          </DialogHeader>
          <TextAreaField name="comment" label={label("comment")} rows={6} />
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
            >
              {t("cancel")}
            </Button>
            <Button
              type="button"
              disabled={isSubmitting}
              onClick={() => void close()}
            >
              {t("plugins.todo.button.close")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
