"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import type { TwoFactorContext, TwoFactorMethods } from "@/lib/rs/auth";
import { isInApp } from "@/lib/app-mode";
import { isWebAuthnSupported } from "@/lib/webauthn";

interface PasskeyRecommendationProps {
  context: TwoFactorContext;
  methods: TwoFactorMethods;
}

/**
 * Strong recommendation to register a passkey, shown beside the 2FA code entry of a user who has none.
 *
 * Not shown in the password reset (the user isn't logged in, no setup page reachable), in the app's WebView
 * (it can't register passkeys) and where the browser can't do WebAuthn. At the login only a pre-login session
 * exists, so the setup page isn't linkable there: a hint names where to find it instead.
 */
export function PasskeyRecommendation({
  context,
  methods,
}: PasskeyRecommendationProps) {
  const t = useTranslations();
  if (
    methods.webAuthn ||
    context === "reset" ||
    isInApp() ||
    !isWebAuthnSupported()
  ) {
    return null;
  }
  return (
    <FormAlert tone="warning">
      <p>{t("user.My2FACode.passkey.recommendation")}</p>
      {context === "session" ? (
        <Link
          href="/2FASetup"
          target="_blank"
          className="font-medium underline underline-offset-2"
        >
          {t("user.My2FACode.passkey.setupLink")}
        </Link>
      ) : (
        <p className="text-pretty text-muted-foreground">
          {t("user.My2FACode.passkey.setupHint")}
        </p>
      )}
    </FormAlert>
  );
}
