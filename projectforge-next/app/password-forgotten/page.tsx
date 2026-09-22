"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { requestPasswordResetMail } from "@/lib/rs/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AuthCard } from "@/components/shared/auth-card";
import { FormAlert } from "@/components/shared/form-alert";

/**
 * Requests a password reset mail. The server never reveals whether the account
 * exists (see PasswordResetService.sendMail), so the confirmation is generic.
 */
export default function PasswordForgottenPage() {
  const t = useTranslations("passwordForgotten");
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const [usernameEmail, setUsernameEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);
    setIsSubmitting(true);
    try {
      const result = await requestPasswordResetMail(usernameEmail);
      if (result.success) {
        setInfo(result.message ?? t("mailSent"));
      } else {
        setError(result.message ?? t("error"));
      }
    } catch {
      setError(t("error"));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthCard title={tb("password.forgotten.title")}>
      <form onSubmit={handleSubmit} className="grid gap-4">
        {error && <FormAlert tone="error">{error}</FormAlert>}
        {info && <FormAlert tone="success">{info}</FormAlert>}
        <div className="grid gap-2">
          <Label htmlFor="usernameEmail">
            {tb("password.reset.username_email")}
          </Label>
          <Input
            id="usernameEmail"
            type="text"
            autoComplete="username"
            autoFocus
            required
            value={usernameEmail}
            onChange={(e) => setUsernameEmail(e.target.value)}
          />
        </div>
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? t("submitting") : tb("password.forgotten.request")}
        </Button>
        <Link
          href="/login"
          className="text-center text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
        >
          {t("backToLogin")}
        </Link>
      </form>
    </AuthCard>
  );
}
