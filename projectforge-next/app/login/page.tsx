"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  loginWithResponse,
  fetchUserStatus,
  cancel2FA,
} from "@/lib/rs/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { TwoFactorForm } from "@/components/shared/two-factor-form";

type Stage = "credentials" | "2fa";

export default function LoginPage() {
  const t = useTranslations("login");
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();

  const [stage, setStage] = useState<Stage>("credentials");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [stayLoggedIn, setStayLoggedIn] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const returnUrl = searchParams.get("returnUrl") || "/";

  async function completeLogin() {
    try {
      await fetchUserStatus();
      await queryClient.invalidateQueries({ queryKey: ["userStatus"] });
      router.push(returnUrl);
    } catch {
      setStage("2fa");
    }
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      const response = await loginWithResponse(username, password, stayLoggedIn);
      if (response.targetType === "CHECK_AUTHENTICATION") {
        await completeLogin();
      } else {
        setError(t("error"));
      }
    } catch {
      setError(t("error"));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (stage === "2fa") {
    return (
      <TwoFactorForm
        onSuccess={async () => {
          await queryClient.invalidateQueries({ queryKey: ["userStatus"] });
          router.push(returnUrl);
        }}
        onCancel={async () => {
          await cancel2FA();
          setStage("credentials");
        }}
      />
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 px-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">{t("title")}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleLogin} className="grid gap-4">
            {error && (
              <div className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
                {error}
              </div>
            )}
            <div className="grid gap-2">
              <Label htmlFor="username">{t("username")}</Label>
              <Input
                id="username"
                type="text"
                autoComplete="username"
                autoFocus
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="password">{t("password")}</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-2">
              <Checkbox
                id="stayLoggedIn"
                checked={stayLoggedIn}
                onCheckedChange={(checked) =>
                  setStayLoggedIn(checked === true)
                }
              />
              <Label htmlFor="stayLoggedIn" className="text-sm font-normal">
                {t("stayLoggedIn")}
              </Label>
            </div>
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? t("submitting") : t("submit")}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
