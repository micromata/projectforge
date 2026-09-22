"use client";

import { Suspense, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  fetchPasswordResetState,
  type PasswordResetState,
} from "@/lib/rs/auth";
import { AuthCard } from "@/components/shared/auth-card";
import { FormAlert } from "@/components/shared/form-alert";
import { NewPasswordForm } from "@/components/shared/new-password-form";
import { TwoFactorForm } from "@/components/shared/two-factor-form";

/**
 * Target of the link mailed by the password-forgotten flow. The token identifies
 * the user; a second factor is required before the new password may be set.
 */
export default function PasswordResetPage() {
  // useSearchParams() must be wrapped in Suspense for the static export build.
  return (
    <Suspense fallback={null}>
      <PasswordReset />
    </Suspense>
  );
}

function PasswordReset() {
  const t = useTranslations("passwordReset");
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  // Set once the 2FA of this page succeeded (the server knows it too, but the
  // status isn't queried again).
  const [twoFactorDone, setTwoFactorDone] = useState(false);
  const [done, setDone] = useState<string | null>(null);

  const { data, isPending } = useQuery<PasswordResetState>({
    queryKey: ["passwordResetState", token],
    queryFn: ({ signal }) => fetchPasswordResetState(token!, signal),
    enabled: !!token,
    retry: false,
    // The token is consumed on success; re-checking it would invalidate the page.
    staleTime: Infinity,
  });
  const state = data;

  if (token && isPending) {
    return (
      <AuthCard title={tb("password.reset.title")}>
        <div className="flex justify-center py-6">
          <div className="size-8 animate-spin rounded-full border-4 border-muted border-t-primary" />
        </div>
      </AuthCard>
    );
  }

  if (done) {
    return (
      <AuthCard title={tb("password.reset.title")}>
        <div className="grid gap-4">
          <FormAlert tone="success">{done}</FormAlert>
          <Link
            href="/login"
            className="text-center text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
          >
            {t("backToLogin")}
          </Link>
        </div>
      </AuthCard>
    );
  }

  if (!token || !state?.tokenValid) {
    return (
      <AuthCard title={tb("password.reset.title")}>
        <div className="grid gap-4">
          <FormAlert tone="error">{tb("password.reset.error")}</FormAlert>
          <Link
            href="/password-forgotten"
            className="text-center text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
          >
            {t("requestNewLink")}
          </Link>
        </div>
      </AuthCard>
    );
  }

  // The server won't accept a new password before a successful 2FA.
  if (!state.twoFactorDone && !twoFactorDone) {
    return (
      <AuthCard title={tb("user.My2FACode.title")}>
        <TwoFactorForm
          context="reset"
          methods={
            state.methods ?? {
              otp: true,
              sms: false,
              mail: false,
              webAuthn: false,
            }
          }
          onSuccess={() => setTwoFactorDone(true)}
        />
      </AuthCard>
    );
  }

  return (
    <AuthCard title={tb("password.reset.title")}>
      <NewPasswordForm
        username={state.username ?? ""}
        csrfToken={state.csrfToken ?? ""}
        onSuccess={setDone}
      />
    </AuthCard>
  );
}
