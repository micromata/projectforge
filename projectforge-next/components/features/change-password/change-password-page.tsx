"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import {
  changePasswordQueryKey,
  fetchChangePasswordInit,
} from "@/lib/rs/change-password";
import { ChangePasswordForm } from "./change-password-form";
import type { PasswordVariant } from "./types";

/**
 * The pages changing the login password (`/next/changePassword`) and the WLAN password
 * (`/next/changeWlanPassword`), successors of the React pages `react/changePassword/dynamic` and
 * `react/changeWlanPassword/dynamic`. With `?userId=`, an admin changes another user's password (linked from
 * the user's edit page); the old password isn't asked then.
 */
export function ChangePasswordPage({ variant }: { variant: PasswordVariant }) {
  const t = useTranslations();
  const userId = useSearchParams().get("userId");
  const query = useQuery({
    queryKey: changePasswordQueryKey(variant, userId),
    queryFn: ({ signal }) => fetchChangePasswordInit(variant, userId, signal),
  });
  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.myAccount")}
        title={
          variant === "wlan"
            ? t("menu.changeWlanPassword")
            : t("menu.changePassword")
        }
      />
      <div className="flex flex-col gap-4 px-4 pb-6 pt-2">
        {query.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {query.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {query.data && (
          // Keyed by the user: another user's page starts with empty fields.
          <ChangePasswordForm
            key={query.data.userId}
            variant={variant}
            init={query.data}
          />
        )}
      </div>
    </PageShell>
  );
}
