"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Button } from "@/components/ui/button";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { fetchMyAccount, MY_ACCOUNT_QUERY_KEY } from "@/lib/rs/my-account";
import { MyAccountForm } from "./my-account-form";

/**
 * The my-account page (`/next/myAccount`), successor of the React page `react/myAccount/dynamic`: the
 * logged-in user's name, personal settings, access tokens, stay-logged-in devices, groups and public keys.
 * Change password, change WLAN password and the 2FA setup are still React pages and linked from here or
 * from the user menu.
 */
export function MyAccountPage() {
  const t = useTranslations();
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
        {/* The pages of the legacy page's menu, still React (MenuItemDefId); the WLAN password only where
            it can be changed. */}
        {query.data && (
          <div className="flex items-center gap-2">
            <PasswordPageButton
              url={query.data.changePasswordUrl}
              label={t("menu.changePassword")}
            />
            <PasswordPageButton
              url={query.data.changeWlanPasswordUrl}
              label={t("menu.changeWlanPassword")}
            />
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
    </PageShell>
  );
}

/** A full page load into the legacy app (see resolveMenuUrl), shaped as a button of the title row. */
function PasswordPageButton({
  url,
  label,
}: {
  url?: string | null;
  label: string;
}) {
  if (!url) return null;
  return (
    <Button variant="outline" asChild>
      <a href={toAbsoluteUrl(resolveMenuUrl(url))}>{label}</a>
    </Button>
  );
}
