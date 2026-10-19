"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormAlert } from "@/components/shared/form-alert";
import { MarkdownText } from "@/components/shared/markdown-text";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { saveMobilePhone } from "@/lib/rs/my-2fa-setup";
import { toast } from "@/lib/toast";
import { HELP_TEXT_CLASS } from "./help-text";
import type { My2FASetupInit } from "./types";
import { runSetupWrite } from "./write-result";

/**
 * The mobile phone for one-time passwords as text messages; only where sms is configured. Saving needs a
 * recent 2FA (asked for by the 403 of the call).
 */
export function MobilePhoneSection({
  init,
  onChanged,
}: {
  init: My2FASetupInit;
  onChanged: () => Promise<void>;
}) {
  const t = useTranslations();
  const [mobilePhone, setMobilePhone] = useState(init.mobilePhone ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const dirty = mobilePhone.trim() !== (init.mobilePhone ?? "");

  const save = async () => {
    setError(null);
    setSaving(true);
    try {
      const saved = await runSetupWrite(
        () => saveMobilePhone(mobilePhone),
        (_, message) => setError(message)
      );
      if (saved) {
        toast.success(t("operation.updated"));
        await onChanged();
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <SectionCard>
      <SectionHeader title={t("user.My2FA.setup.sms.info.title")} />
      <div className="flex flex-col gap-3 text-sm">
        <MarkdownText
          text={t("user.My2FA.setup.sms.info._")}
          className={HELP_TEXT_CLASS}
        />
        {!init.mobilePhone && (
          <FormAlert tone="error">
            <MarkdownText
              text={t("user.My2FA.setup.sms.mobileNumberRecommended")}
            />
          </FormAlert>
        )}
        <form
          className="flex flex-col gap-1.5"
          onSubmit={(event) => {
            event.preventDefault();
            void save();
          }}
        >
          <Label htmlFor="my2fa-mobilePhone">{t("user.mobilePhone._")}</Label>
          <div className="flex items-center gap-2">
            <Input
              id="my2fa-mobilePhone"
              name="mobilePhone"
              type="tel"
              autoComplete="tel"
              value={mobilePhone}
              aria-invalid={error ? true : undefined}
              onChange={(event) => {
                setMobilePhone(event.target.value);
                setError(null);
              }}
              className="max-w-xs"
            />
            <Button type="submit" disabled={saving || !dirty}>
              {t("save")}
            </Button>
          </div>
          {error && <p className="text-xs text-destructive">{error}</p>}
        </form>
      </div>
    </SectionCard>
  );
}
