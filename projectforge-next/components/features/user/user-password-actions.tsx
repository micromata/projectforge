"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ChangePasswordDialog } from "@/components/features/change-password/change-password-dialog";
import type { PasswordVariant } from "@/components/features/change-password/types";
import type { UserDetail } from "./types";

/**
 * Beside the heading of a stored user's form: change the password (and the WLAN password, where
 * supported) in a dialog — the action an administrator comes here for most, hence at the very top and
 * highlighted. Nothing a save would write, so independent of the form. A new user has no password to
 * change yet; it is set in the form (UserPasswordFields).
 */
export function UserPasswordActions({ user }: { user?: UserDetail }) {
  const t = useTranslations();
  const [passwordDialog, setPasswordDialog] = useState<PasswordVariant | null>(
    null
  );
  const userId = user?.id;
  if (userId == null) return null;
  return (
    <>
      <Button
        type="button"
        size="sm"
        onClick={() => setPasswordDialog("password")}
      >
        {t("menu.changePassword")}
      </Button>
      {user?.wlanPasswordSupported && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setPasswordDialog("wlan")}
        >
          {t("menu.changeWlanPassword")}
        </Button>
      )}
      {passwordDialog && (
        <ChangePasswordDialog
          variant={passwordDialog}
          userId={userId}
          onClose={() => setPasswordDialog(null)}
        />
      )}
    </>
  );
}
