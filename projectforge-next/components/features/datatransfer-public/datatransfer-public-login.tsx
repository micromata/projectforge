"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AuthCard } from "@/components/shared/auth-card";
import { FormAlert } from "@/components/shared/form-alert";
import {
  loginDataTransferPublic,
  type DataTransferPublicState,
} from "@/lib/rs/datatransfer-public";

/**
 * The password of the area's external access, and optionally who is asking: the name or e-mail goes
 * into the area's activities next to the ip address, so its owners know whose files they got.
 *
 * @param notice shown above the form in place of an error, e.g. "logged out" after a logout.
 */
export function DataTransferPublicLogin({
  token,
  title,
  notice,
  onLoggedIn,
}: {
  token: string;
  title: string;
  notice?: string | null;
  onLoggedIn: (state: DataTransferPublicState) => void;
}) {
  const t = useTranslations();
  const [password, setPassword] = useState("");
  const [userInfo, setUserInfo] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const result = await loginDataTransferPublic(token, password, userInfo);
      if (result.kind === "ok") {
        onLoggedIn(result.state);
      } else {
        setError(result.message || t("login.error.loginFailed"));
        setPassword("");
      }
    } catch {
      setError(t("login.error.loginFailed"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthCard title={title}>
      <form onSubmit={handleSubmit} className="grid gap-4">
        {error ? (
          <FormAlert tone="error">{error}</FormAlert>
        ) : (
          notice && <FormAlert tone="success">{notice}</FormAlert>
        )}
        <div className="grid gap-2">
          <Label htmlFor="externalPassword">{t("password._")}</Label>
          <Input
            id="externalPassword"
            type="password"
            autoComplete="current-password"
            autoFocus
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="userInfo">
            {t("plugins.datatransfer.external.userInfo._")}
          </Label>
          <Input
            id="userInfo"
            type="text"
            autoComplete="email"
            maxLength={255}
            value={userInfo}
            onChange={(event) => setUserInfo(event.target.value)}
          />
          <p className="text-xs text-muted-foreground">
            {t("plugins.datatransfer.external.userInfo.info")}
          </p>
        </div>
        <Button type="submit" className="w-full" disabled={submitting}>
          {t("login._")}
        </Button>
      </form>
    </AuthCard>
  );
}
