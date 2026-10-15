"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/shared/spinner";
import {
  changePasswordQueryKey,
  fetchChangePasswordInit,
} from "@/lib/rs/change-password";
import { ChangePasswordFieldset } from "./change-password-form";
import type { ChangePasswordInit, PasswordVariant } from "./types";
import { useChangePassword } from "./use-change-password";

/**
 * The password change as a modal over the page it is opened from — the my-account page for the user's own
 * password, the user's edit page for an admin changing another user's (`userId`). The same fields and
 * rules as the page `/next/changePassword` (`/next/changeWlanPassword`), which stays for the menu; closes
 * after a successful change.
 */
export function ChangePasswordDialog({
  variant,
  userId,
  onClose,
}: {
  variant: PasswordVariant;
  /** The user whose password an admin changes; omitted for the own one. */
  userId?: number;
  onClose: () => void;
}) {
  const t = useTranslations();
  const id = userId != null ? String(userId) : null;
  const query = useQuery({
    queryKey: changePasswordQueryKey(variant, id),
    queryFn: ({ signal }) => fetchChangePasswordInit(variant, id, signal),
  });
  const title =
    variant === "wlan"
      ? t("menu.changeWlanPassword")
      : t("menu.changePassword");

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription className="sr-only">{title}</DialogDescription>
        </DialogHeader>
        {query.isPending ? (
          <div className="flex justify-center py-4">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        ) : query.isError ? (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        ) : (
          <ChangePasswordDialogForm
            variant={variant}
            init={query.data}
            onClose={onClose}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}

/** The form inside the dialog, mounted once the initial data is there (useChangePassword needs it). */
function ChangePasswordDialogForm({
  variant,
  init,
  onClose,
}: {
  variant: PasswordVariant;
  init: ChangePasswordInit;
  onClose: () => void;
}) {
  const t = useTranslations();
  const state = useChangePassword(variant, init, onClose);
  const { change } = state;
  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        // The page's form may be around the dialog in the React tree (portal): not its submit.
        event.stopPropagation();
        if (!change.isPending) change.mutate();
      }}
    >
      <ChangePasswordFieldset variant={variant} init={init} state={state} />
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          {t("cancel")}
        </Button>
        {init.supported && (
          <Button type="submit" disabled={change.isPending}>
            {t("update")}
          </Button>
        )}
      </DialogFooter>
    </form>
  );
}
