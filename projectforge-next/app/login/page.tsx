"use client";

import { Suspense, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  cancelLogin,
  fetchLoginState,
  login,
  type TwoFactorMethods,
} from "@/lib/rs/auth";
import {
  resolveMenuUrl,
  sanitizeRedirectUrl,
  toAbsoluteUrl,
} from "@/lib/menu-url";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { AuthCard } from "@/components/shared/auth-card";
import { FormAlert } from "@/components/shared/form-alert";
import { TwoFactorForm } from "@/components/shared/two-factor-form";

export default function LoginPage() {
  // useSearchParams() must be wrapped in Suspense for the static export build.
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const t = useTranslations("login");
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [stayLoggedIn, setStayLoggedIn] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [motd, setMotd] = useState<string | null>(null);
  // Set once username/password were accepted but a second factor is missing.
  const [methods, setMethods] = useState<TwoFactorMethods | null>(null);

  // Where the user wanted to go, as WicketUserFilter/WicketUtils and the legacy
  // React app hand it over. Caller-supplied, so it must stay on this server (open
  // redirect otherwise) - as must the server's redirectUrl.
  const returnUrl = sanitizeRedirectUrl(searchParams.get("returnUrl"));

  /**
   * Follows the redirect target if it points at another frontend, otherwise routes
   * inside this app.
   *
   * The requested url wins over the server's, and the server has no say in it: a
   * successful login rotates the http session (LoginService.internalLogin, session
   * fixation), so nothing the server stored before the login survives it. The legacy
   * login form carried the url through that rotation in `serverData`; here it simply
   * stays in the address bar, through the 2FA step as well. The server's `redirectUrl`
   * is therefore only the default for a login opened without one - and that default is
   * `/react/calendar`, not this app's start page, so don't substitute "/" for it.
   */
  const goTo = useCallback(
    async (redirectUrl?: string | null) => {
      await queryClient.invalidateQueries({ queryKey: ["userStatus"] });
      const safeUrl = returnUrl ?? sanitizeRedirectUrl(redirectUrl);
      if (!safeUrl) {
        router.push("/");
        return;
      }
      const target = resolveMenuUrl(safeUrl);
      if (target.kind === "internal") {
        router.push(target.href);
      } else {
        window.location.assign(toAbsoluteUrl(target));
      }
    },
    [queryClient, returnUrl, router]
  );

  // The session may already be logged-in, or pre-logged-in with a pending
  // second factor (e.g. after a browser reload during the 2FA step).
  useEffect(() => {
    let cancelled = false;
    fetchLoginState()
      .then((state) => {
        if (cancelled) return;
        if (state.setupRedirectUrl) {
          window.location.assign(state.setupRedirectUrl);
          return;
        }
        setMotd(state.messageOfTheDay ?? null);
        if (state.twoFactorRequired) {
          setMethods(state.methods ?? null);
        } else if (state.loggedIn) {
          void goTo(null);
        }
      })
      .catch(() => {
        /* The login form works without the state, so stay silent. */
      });
    return () => {
      cancelled = true;
    };
  }, [goTo, returnUrl]);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      const result = await login(username, password, stayLoggedIn);
      if (result.status === "SUCCESS") {
        await goTo(result.redirectUrl);
      } else if (result.status === "TWO_FACTOR_REQUIRED") {
        setMethods(
          result.methods ?? {
            otp: true,
            sms: false,
            mail: false,
            webAuthn: false,
          }
        );
      } else {
        setError(result.message ?? tb("login.error.loginFailed"));
      }
    } catch {
      setError(tb("login.error.loginFailed"));
    } finally {
      setIsSubmitting(false);
    }
  }

  if (methods) {
    return (
      <AuthCard title={tb("user.My2FACode.title")}>
        <TwoFactorForm
          context="login"
          methods={methods}
          onSuccess={(result) => void goTo(result.redirectUrl)}
          onCancel={async () => {
            await cancelLogin();
            setMethods(null);
            setPassword("");
          }}
        />
      </AuthCard>
    );
  }

  return (
    <AuthCard title={tb("login.title")}>
      <form onSubmit={handleLogin} className="grid gap-4">
        {motd && <FormAlert tone="info">{motd}</FormAlert>}
        {error && <FormAlert tone="error">{error}</FormAlert>}
        <div className="grid gap-2">
          <Label htmlFor="username">{tb("username")}</Label>
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
          <Label htmlFor="password">{tb("password._")}</Label>
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
            onCheckedChange={(checked) => setStayLoggedIn(checked === true)}
          />
          <Label htmlFor="stayLoggedIn" className="text-sm font-normal">
            {tb("login.stayLoggedIn._")}
          </Label>
        </div>
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? t("submitting") : tb("login._")}
        </Button>
        <Link
          href="/password-forgotten"
          className="text-center text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
        >
          {tb("password.forgotten.link")}
        </Link>
      </form>
    </AuthCard>
  );
}
