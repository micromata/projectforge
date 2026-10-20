"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { CopyableValue } from "@/components/shared/copyable-value";
import { FormAlert } from "@/components/shared/form-alert";
import { ReadOnlyValue } from "@/components/shared/form/read-only-value";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { MarkdownText } from "@/components/shared/markdown-text";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { RsError } from "@/lib/rs/client";
import {
  disableAuthenticatorApp,
  enableAuthenticatorApp,
  fetchAuthenticatorKey,
} from "@/lib/rs/my-2fa-setup";
import { toast } from "@/lib/toast";
import type { My2FAAuthenticatorKey, My2FASetupInit } from "./types";
import { runSetupWrite } from "./write-result";

/**
 * The authenticator app (Microsoft or Google Authenticator): enabled by creating its key, disabled (or
 * renewed, by enabling again) by deleting it. The key and its QR code are shown on demand only; like
 * every change here, that needs a recent 2FA.
 */
export function AuthenticatorSection({
  init,
  onChanged,
}: {
  init: My2FASetupInit;
  onChanged: () => Promise<void>;
}) {
  const t = useTranslations();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [authenticatorKey, setAuthenticatorKey] =
    useState<My2FAAuthenticatorKey | null>(null);

  const showKey = async (show: boolean) => {
    if (!show) {
      setAuthenticatorKey(null);
      return;
    }
    setBusy(true);
    try {
      setAuthenticatorKey(await fetchAuthenticatorKey());
    } catch (error) {
      // 403: the 2FA dialog was cancelled.
      if (!(error instanceof RsError && error.status === 403)) {
        toast.error(error instanceof Error ? error.message : String(error));
      }
    } finally {
      setBusy(false);
    }
  };

  const toggleApp = async () => {
    setBusy(true);
    try {
      const enable = !init.authenticatorConfigured;
      const done = await runSetupWrite(() =>
        enable ? enableAuthenticatorApp() : disableAuthenticatorApp()
      );
      if (done) {
        await onChanged();
        // A new key is to be scanned right away (the legacy page showed it, too).
        if (enable) {
          await showKey(true);
        } else {
          setAuthenticatorKey(null);
        }
      }
    } finally {
      setBusy(false);
    }
  };

  const enabled = init.authenticatorConfigured;
  const actionLabel = enabled
    ? t("user.My2FA.setup.disableAuthenticatorApp._")
    : t("user.My2FA.setup.enableAuthenticatorApp._");
  return (
    <SectionCard>
      <SectionHeader title={t("user.My2FA.setup.authenticator.title")} />
      <div className="flex flex-col gap-4 text-sm">
        {!enabled && (
          <FormAlert tone="error">
            <MarkdownText text={t("user.My2FA.setup.authenticator.intro")} />
          </FormAlert>
        )}
        <div className="flex flex-wrap items-end gap-4">
          <HintTooltip
            text={
              enabled
                ? t("user.My2FA.setup.disableAuthenticatorApp.info")
                : t("user.My2FA.setup.enableAuthenticatorApp.info")
            }
          >
            <Button
              type="button"
              variant="outline"
              className="text-destructive"
              disabled={busy}
              onClick={() => setConfirmOpen(true)}
            >
              {actionLabel}
            </Button>
          </HintTooltip>
          {enabled && (
            <ReadOnlyValue label={t("created")}>
              {init.authenticatorKeyCreated || "–"}
            </ReadOnlyValue>
          )}
        </div>
        {enabled && (
          <div className="flex items-center gap-2">
            <Switch
              id="my2fa-showAuthenticatorKey"
              checked={authenticatorKey !== null}
              disabled={busy}
              onCheckedChange={(checked) => void showKey(checked)}
            />
            <Label htmlFor="my2fa-showAuthenticatorKey">
              {t("user.My2FA.setup.showAuthenticatorKey")}
            </Label>
          </div>
        )}
        {enabled && authenticatorKey && (
          <div className="flex flex-col gap-3">
            <FormAlert tone="success">
              <MarkdownText text={t("user.My2FA.setup.authenticator.info")} />
            </FormAlert>
            <ReadOnlyValue label={t("user.My2FA.setup.authenticatorKey")}>
              <CopyableValue
                value={authenticatorKey.key}
                label={t("user.My2FA.setup.authenticatorKey")}
              />
            </ReadOnlyValue>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={authenticatorKey.qrCodeUrl}
              alt={t("user.My2FA.setup.authenticatorKey")}
              className="size-48 self-start rounded-md border bg-white p-2"
            />
          </div>
        )}
      </div>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={actionLabel}
        description={
          enabled
            ? t("user.My2FA.setup.disableAuthenticatorApp.confirmMessage")
            : t("user.My2FA.setup.enableAuthenticatorApp.confirmMessage")
        }
        confirmLabel={actionLabel}
        destructive
        onConfirm={() => void toggleApp()}
      />
    </SectionCard>
  );
}
