"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { ChangePasswordDialog } from "@/components/features/change-password/change-password-dialog";
import type { PasswordVariant } from "@/components/features/change-password/types";
import { fetchMyAccount, MY_ACCOUNT_QUERY_KEY } from "@/lib/rs/my-account";
import { MyAccountForm } from "./my-account-form";

/**
 * The my-account page (`/next/myAccount`), successor of the React page `react/myAccount/dynamic`: the
 * logged-in user's name, personal settings, access tokens, stay-logged-in devices, groups and public keys.
 * Change password and change WLAN password open as dialogs from here; the 2FA setup is still a React page,
 * linked from the form and from the user menu.
 */
export function MyAccountPage() {
  const t = useTranslations();
  const [passwordDialog, setPasswordDialog] = useState<PasswordVariant | null>(
    null
  );
  const query = useQuery({
    queryKey: MY_ACCOUNT_QUERY_KEY,
    queryFn: ({ signal }) => fetchMyAccount(signal),
  });
  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.myAccount")}
        title={t("user.myAccount.title.edit")}
      >
        {/* The entries of the legacy page's menu (MenuItemDefId), offered where the backend gives their
            url (menu visibility; the WLAN password only where it can be changed), opened as dialogs. */}
        {query.data && (
          <div className="flex items-center gap-2">
            {query.data.changePasswordUrl && (
              <Button
                variant="outline"
                onClick={() => setPasswordDialog("password")}
              >
                {t("menu.changePassword")}
              </Button>
            )}
            {query.data.changeWlanPasswordUrl && (
              <Button
                variant="outline"
                onClick={() => setPasswordDialog("wlan")}
              >
                {t("menu.changeWlanPassword")}
              </Button>
            )}
          </div>
        )}
      </PageTitleRow>
      {query.isPending && (
        <p className="px-4 text-sm text-muted-foreground">{t("loading")}</p>
      )}
      {query.isError && (
        <p className="px-4 text-sm text-destructive">
          {t("access.exception.noAccess")}
        </p>
      )}
      {query.data && <MyAccountForm init={query.data} />}
      {passwordDialog && (
        <ChangePasswordDialog
          variant={passwordDialog}
          onClose={() => setPasswordDialog(null)}
        />
      )}
    </PageShell>
  );
}
