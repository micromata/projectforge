"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { SecurityCheckIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/shared/form-alert";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { MarkdownText } from "@/components/shared/markdown-text";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { requestSecondFactor } from "@/lib/rs/client";
import { fetch2FASetup, MY_2FA_SETUP_QUERY_KEY } from "@/lib/rs/my-2fa-setup";
import { AuthenticatorSection } from "./authenticator-section";
import { HELP_TEXT_CLASS } from "./help-text";
import { MobilePhoneSection } from "./mobile-phone-section";
import type { My2FASetupInit } from "./types";
import { WebAuthnSection } from "./webauthn-section";

/**
 * The 2FA setup of the logged-in user (`/next/2FASetup`), successor of the React pages
 * `react/2FASetup/dynamic` and `react/WebAuthnSetup/dynamic` (a WebAuthn key is edited in a dialog now).
 *
 * The page shows the state; the authenticator key and every change need a 2FA not older than
 * `fresh2FAMinutes`, which the backend demands per call (see lib/rs/my-2fa-setup.ts). "Check 2FA" does it
 * up front, and doubles as the legacy page's place to try a code.
 */
export function My2FASetupPage() {
  const t = useTranslations();
  const query = useQuery({
    queryKey: MY_2FA_SETUP_QUERY_KEY,
    queryFn: ({ signal }) => fetch2FASetup(signal),
  });
  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.myAccount")}
        title={t("user.My2FA.setup.title")}
      />
      {query.isPending && (
        <p className="px-4 text-sm text-muted-foreground">{t("loading")}</p>
      )}
      {query.isError && (
        <p className="px-4 text-sm text-destructive">
          {t("access.exception.noAccess")}
        </p>
      )}
      {query.data && <My2FASetupContent init={query.data} />}
    </PageShell>
  );
}

function My2FASetupContent({ init }: { init: My2FASetupInit }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [checking, setChecking] = useState(false);
  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: MY_2FA_SETUP_QUERY_KEY });

  /** Asks for the second factor now; resolves to false if cancelled. */
  const check2FA = async (): Promise<boolean> => {
    setChecking(true);
    try {
      const success = await requestSecondFactor(
        init.fresh2FAMinutes * 60 * 1000
      );
      if (success) await refresh();
      return success;
    } finally {
      setChecking(false);
    }
  };

  return (
    <div className="flex flex-col gap-4 px-4 pt-2 pb-6">
      <SectionCard>
        <SectionHeader title={t("user.My2FA.setup.info.title")} />
        <div className="flex flex-col gap-3 text-sm">
          <MarkdownText
            text={t("user.My2FA.setup.info.1")}
            className={HELP_TEXT_CLASS}
          />
          {!init.userConfigured2FA && (
            <FormAlert tone="error">
              <MarkdownText text={t("user.My2FA.setup.info.2")} />
            </FormAlert>
          )}
          <MarkdownText
            text={t("user.My2FA.setup.info.3")}
            className={HELP_TEXT_CLASS}
          />
          <div className="flex flex-wrap items-end gap-4">
            <ReadOnlyValue label={t("user.My2FACode.lastSuccessful2FA")}>
              {init.lastSuccessful2FA || "–"}
            </ReadOnlyValue>
            <Button
              type="button"
              variant="outline"
              disabled={checking}
              onClick={() => void check2FA()}
            >
              <HugeiconsIcon icon={SecurityCheckIcon} size={14} aria-hidden />
              {t("user.My2FA.setup.check2FA")}
            </Button>
          </div>
        </div>
      </SectionCard>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="flex flex-col gap-4">
          {init.smsConfigured && (
            <MobilePhoneSection init={init} onChanged={refresh} />
          )}
          <WebAuthnSection
            init={init}
            onChanged={refresh}
            check2FA={check2FA}
          />
        </div>
        <AuthenticatorSection init={init} onChanged={refresh} />
      </div>
    </div>
  );
}
