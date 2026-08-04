"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  checkOtp,
  sendMailCode,
  sendSmsCode,
  type TwoFactorContext,
  type TwoFactorMethods,
  type TwoFactorResult,
} from "@/lib/rs/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormAlert } from "@/components/shared/form-alert";
import { WebAuthnButton } from "@/components/shared/web-authn-button";

interface TwoFactorFormProps {
  context: TwoFactorContext;
  /**
   * Which second factors the server accepts for this user. Only these are
   * offered — the backend would reject the others.
   */
  methods: TwoFactorMethods;
  /**
   * Hint why a factor is needed now (in-session 2FA: "your last check is old").
   */
  hint?: string | null;
  onSuccess: (result: TwoFactorResult) => void;
  onCancel?: () => void;
}

export function TwoFactorForm({
  context,
  methods,
  hint,
  onSuccess,
  onCancel,
}: TwoFactorFormProps) {
  const t = useTranslations("login.twoFactor");
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const [code, setCode] = useState("");
  // Only needed as additional factor when the OTP was mailed (see My2FAData.password).
  const [password, setPassword] = useState("");
  const [mailCodeSent, setMailCodeSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    setIsSubmitting(true);
    try {
      const result = await checkOtp(
        context,
        code,
        mailCodeSent ? password : undefined
      );
      if (result.success) {
        onSuccess(result);
      } else {
        setError(result.message ?? tb("user.My2FACode.error.validation"));
      }
    } catch {
      setError(tb("user.My2FACode.error.validation"));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function requestCode(via: "sms" | "mail") {
    setError(null);
    setInfo(null);
    try {
      const result =
        via === "sms"
          ? await sendSmsCode(context)
          : await sendMailCode(context as "login" | "session");
      if (result.success) {
        setInfo(result.message ?? t(via === "sms" ? "smsSent" : "mailSent"));
        if (via === "mail") setMailCodeSent(true);
      } else {
        setError(result.message ?? tb("user.My2FACode.error.validation"));
      }
    } catch {
      setError(tb("user.My2FACode.error.validation"));
    }
  }

  return (
    <form onSubmit={handleVerify} className="grid gap-4">
      {hint && <FormAlert tone="info">{hint}</FormAlert>}
      <p className="text-sm text-muted-foreground">
        {tb("user.My2FACode.authentification.info")}
      </p>
      {error && <FormAlert tone="error">{error}</FormAlert>}
      {info && <FormAlert tone="success">{info}</FormAlert>}
      {methods.lastSuccessful2FA && (
        <p className="text-xs text-muted-foreground">
          {`${tb("user.My2FACode.lastSuccessful2FA")}: ${methods.lastSuccessful2FA}`}
        </p>
      )}
      <div className="grid gap-2">
        <Label htmlFor="otp-code">{tb("user.My2FACode.code._")}</Label>
        <Input
          id="otp-code"
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          autoFocus
          required
          value={code}
          onChange={(e) => setCode(e.target.value)}
        />
      </div>
      {mailCodeSent && (
        <div className="grid gap-2">
          <Label htmlFor="otp-password">{tb("password._")}</Label>
          <Input
            id="otp-password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
      )}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" disabled={isSubmitting || !code}>
          {isSubmitting ? t("verifying") : tb("user.My2FACode.code.validate")}
        </Button>
        {methods.sms && (
          <Button
            type="button"
            variant="outline"
            onClick={() => requestCode("sms")}
          >
            {tb("user.My2FACode.sendCode.sms._")}
          </Button>
        )}
        {methods.mail && (
          <Button
            type="button"
            variant="outline"
            onClick={() => requestCode("mail")}
          >
            {tb("user.My2FACode.sendCode.mail._")}
          </Button>
        )}
        {methods.webAuthn && (
          <WebAuthnButton
            context={context}
            autoStart={context === "login"}
            onSuccess={onSuccess}
            onError={setError}
          />
        )}
        {onCancel && (
          <Button type="button" variant="ghost" onClick={onCancel}>
            {tb("cancel")}
          </Button>
        )}
      </div>
    </form>
  );
}
