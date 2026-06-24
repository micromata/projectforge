"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { checkOtp, sendSmsCode, sendMailCode } from "@/lib/rs/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface TwoFactorFormProps {
  onSuccess: () => void;
  onCancel: () => void;
}

export function TwoFactorForm({ onSuccess, onCancel }: TwoFactorFormProps) {
  const t = useTranslations("login.twoFactor");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleVerify(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const response = await checkOtp(code);
      if (response.targetType === "CHECK_AUTHENTICATION") {
        onSuccess();
      } else {
        setError(t("error"));
      }
    } catch {
      setError(t("error"));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSendSms() {
    try {
      await sendSmsCode();
      setInfo(t("smsSent"));
    } catch {
      /* ignore */
    }
  }

  async function handleSendMail() {
    try {
      await sendMailCode();
      setInfo(t("mailSent"));
    } catch {
      /* ignore */
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-xl">{t("title")}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="mb-4 rounded-md bg-sky-50 px-3 py-2 text-sm text-muted-foreground dark:bg-sky-950">
            {t("description")}
          </div>
          <form onSubmit={handleVerify} className="grid gap-4">
            {error && (
              <div className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
                {error}
              </div>
            )}
            {info && (
              <div className="rounded-md bg-green-50 px-3 py-2 text-sm text-green-700 dark:bg-green-950 dark:text-green-300">
                {info}
              </div>
            )}
            <div className="grid gap-2">
              <Label htmlFor="otp-code">{t("code")}</Label>
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
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="destructive"
                onClick={onCancel}
              >
                {t("cancel")}
              </Button>
              <Button type="submit" disabled={isSubmitting || !code}>
                {isSubmitting ? t("verifying") : t("verify")}
              </Button>
              <Button type="button" variant="outline" onClick={handleSendSms}>
                {t("sendSms")}
              </Button>
              <Button type="button" variant="link" onClick={handleSendMail}>
                {t("sendMail")}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
