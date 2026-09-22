"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { setNewPassword } from "@/lib/rs/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { FormAlert } from "@/components/shared/form-alert";

interface NewPasswordFormProps {
  username: string;
  /** Taken from the reset status, needed for the final post. */
  csrfToken: string;
  onSuccess: (message: string) => void;
}

/**
 * The two password fields of the reset flow. Only shown after a successful
 * second factor — the server enforces that as well.
 */
export function NewPasswordForm({
  username,
  csrfToken,
  onSuccess,
}: NewPasswordFormProps) {
  const t = useTranslations("passwordReset");
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const [password, setPassword] = useState("");
  const [passwordRepeat, setPasswordRepeat] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const result = await setNewPassword(password, passwordRepeat, csrfToken);
      if (result.success) {
        onSuccess(
          result.message ??
            tb("user.changePassword.msg.passwordSuccessfullyChanged")
        );
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
    <form onSubmit={handleSubmit} className="grid gap-4">
      {error && <FormAlert tone="error">{error}</FormAlert>}
      <div className="grid gap-2">
        <Label htmlFor="reset-username">{tb("username")}</Label>
        <Input
          id="reset-username"
          type="text"
          value={username}
          readOnly
          disabled
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="new-password">
          {tb("user.changePassword.newPassword")}
        </Label>
        <Input
          id="new-password"
          type="password"
          autoComplete="new-password"
          autoFocus
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="new-password-repeat">{tb("passwordRepeat")}</Label>
        <Input
          id="new-password-repeat"
          type="password"
          autoComplete="new-password"
          required
          value={passwordRepeat}
          onChange={(e) => setPasswordRepeat(e.target.value)}
        />
      </div>
      <Button type="submit" className="w-full" disabled={isSubmitting}>
        {isSubmitting ? t("submitting") : t("submit")}
      </Button>
    </form>
  );
}
