"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import type { TwoFactorContext, TwoFactorMethods } from "@/lib/rs/auth";
import { Button } from "@/components/ui/button";
import { appOpensPageInBrowser, isInApp } from "@/lib/app-mode";
import { openPageInBrowser } from "@/lib/app-bridge";
import { isWebAuthnSupported } from "@/lib/webauthn";

interface PasskeyRecommendationProps {
  context: TwoFactorContext;
  methods: TwoFactorMethods;
}

/**
 * Strong recommendation to register a passkey, shown beside the 2FA code entry of a user who has none.
 *
 * Not shown in the password reset (the user isn't logged in, no setup page reachable), in the app's WebView
 * (it can't register passkeys; an app that opens the setup in the system browser gets a button for that) and where
 * the browser can't do WebAuthn. At the login only a pre-login session
 * exists, so the setup page isn't linkable there: a hint names where to find it instead.
 */
export function PasskeyRecommendation({
  context,
  methods,
}: PasskeyRecommendationProps) {
  const t = useTranslations();
  // Only where the setup page is reachable (not at the login, where it has no button).
  const inBrowser = context === "session" && appOpensPageInBrowser();
  if (
    methods.webAuthn ||
    context === "reset" ||
    (isInApp() && !inBrowser) ||
    // The system browser does the registration, whatever the WebView can.
    (!inBrowser && !isWebAuthnSupported())
  ) {
    return null;
  }
  return (
    <FormAlert tone="warning">
      <p>{t("user.My2FACode.passkey.recommendation")}</p>
      {context === "session" && inBrowser ? (
        <Button
          type="button"
          variant="link"
          className="h-auto p-0 font-medium"
          onClick={() => openPageInBrowser("/next/2FASetup")}
        >
          {t("webauthn.browser.open")}
        </Button>
      ) : context === "session" ? (
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
