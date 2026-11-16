"use client";

import { useRef, useState } from "react";
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
import { AppBrowser2FAButton } from "@/components/shared/app-browser-2fa-button";
import { appConfirms2FAInBrowser, isInApp } from "@/lib/app-mode";

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
  /** In-session 2FA: how fresh the factor must be (`expiryMillis` of the refused request). */
  expiryMillis?: number | null;
  /** Start WebAuthn right away (default: in the login context only). */
  autoStartWebAuthn?: boolean;
  onSuccess: (result: TwoFactorResult) => void;
  onCancel?: () => void;
}

export function TwoFactorForm({
  context,
  methods,
  hint,
  expiryMillis,
  autoStartWebAuthn = context === "login",
  onSuccess,
  onCancel,
}: TwoFactorFormProps) {
  const t = useTranslations("login.twoFactor");
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const [code, setCode] = useState("");
  const codeInputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  // The WebView of the app can't do WebAuthn (no associated domain): the app confirms in the browser instead. The
  // login and reset pages of the app login run in the browser itself.
  const inAppSession = context === "session" && isInApp();

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    setIsSubmitting(true);
    try {
      // No password: it is only an additional factor for a mailed OTP if the server asks for it
      // (My2FAData.password), and that option is switched off server-side
      // (My2FARequestConfiguration.checkLoginPasswordRequired4Mail2FA). Should it come back, the
      // server has to announce it in TwoFactorMethods - it isn't a client-side guess.
      const result = await checkOtp(context, code);
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
      } else {
        setError(result.message ?? tb("user.My2FACode.error.validation"));
      }
    } catch {
      setError(tb("user.My2FACode.error.validation"));
    } finally {
      // The click moved focus to the button — hand it back so the user can type
      // the freshly sent code without reaching for the mouse.
      codeInputRef.current?.focus();
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
          ref={codeInputRef}
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          autoFocus
          required
          value={code}
          onChange={(e) => setCode(e.target.value)}
        />
      </div>
      <div className="flex flex-wrap gap-2">
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel}>
            {tb("cancel")}
          </Button>
        )}
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
        {inAppSession && appConfirms2FAInBrowser() && (
          <AppBrowser2FAButton
            maxAgeMillis={expiryMillis}
            onSuccess={() => onSuccess({ success: true })}
            onError={setError}
          />
        )}
        {methods.webAuthn && !inAppSession && (
          <WebAuthnButton
            context={context}
            autoStart={autoStartWebAuthn}
            onSuccess={onSuccess}
            onError={setError}
          />
        )}
      </div>
    </form>
  );
}
