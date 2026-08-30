"use client";

import { useTranslations } from "next-intl";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  resolveUnsavedChanges,
  useUnsavedChangesRequest,
} from "@/hooks/use-unsaved-changes-warning";

/**
 * The one "there are unsaved changes — leave anyway?" dialog, in place of the browser's `window.confirm`
 * box. Mounted once at the app root so any link or modal leaving a dirty edit form can raise it (see
 * confirmLeaveUnsavedChanges); the description is the same backend text the browser showed, only now in
 * the app's own dialog.
 */
export function UnsavedChangesDialogHost() {
  const t = useTranslations();
  const request = useUnsavedChangesRequest();

  return (
    <ConfirmDialog
      open={request !== null}
      // Cancel, ESC and the overlay all close without leaving — the safe answer for a dirty form.
      onOpenChange={(open) => {
        if (!open) resolveUnsavedChanges(false);
      }}
      title={t("unsavedChanges.title")}
      description={request?.message ?? ""}
      confirmLabel={t("unsavedChanges.confirm")}
      cancelLabel={t("unsavedChanges.stay")}
      onConfirm={() => resolveUnsavedChanges(true)}
    />
  );
}
