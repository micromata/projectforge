"use client";

import { useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { AuthCard } from "@/components/shared/auth-card";
import { useAuth } from "@/hooks/use-auth";
import { logout, RsError } from "@/lib/rs/client";
import { appCancelUrl, authorizeApp } from "@/lib/rs/app-auth";
import { toast } from "@/lib/toast";
import { isSecondFactorRenewal, readAppLoginParams } from "./app-login-params";
import { AppLoginSecondFactor } from "./app-login-second-factor";

type Status = "ask" | "done" | "cancelled" | "invalid";

/**
 * Opened by the ProjectForge app in the system browser (`/appLogin?login=…&challenge=…&state=…`, see
 * readAppLoginParams): after the normal login (AuthGuard) the user confirms the app, the server demands a
 * fresh second factor, and the browser hands the one-time code to the app via its callback url
 * (AppAuthService). The explicit
 * confirmation keeps a link sent by somebody else from connecting an app unnoticed. For the second factor of a
 * page of the app (isSecondFactorRenewal) the second factor itself is that confirmation.
 *
 * The browser session outlives a logout in the app, so the page names the user logged in here and offers
 * "Other user": it ends the browser session and shows the login, which returns here with the same parameters.
 */
export function AppLoginPage({
  packedLogin = null,
}: {
  /** The packed parameters of the path (`/appLogin/<login>`), see readAppLoginParams. */
  packedLogin?: string | null;
} = {}) {
  const t = useTranslations();
  const params = readAppLoginParams(useSearchParams(), packedLogin);
  const { challenge, state, deviceName, platform, deviceId, max2FAAgeMillis } =
    params;
  const [status, setStatus] = useState<Status>(
    challenge && state ? "ask" : "invalid"
  );
  const { user } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const queryClient = useQueryClient();

  // The browser session outlives the logout in the app and would log the same user in again: this ends it and
  // shows the login, which brings the user back here (the query holds the challenge of the app).
  const switchUser = useMutation({
    mutationFn: () => logout(),
    onSuccess: () => {
      queryClient.clear();
      const returnUrl = encodeURIComponent(pathname + window.location.search);
      router.replace(`/login?returnUrl=${returnUrl}`);
    },
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  const authorize = useMutation({
    mutationFn: () =>
      authorizeApp({
        challenge: challenge!,
        state: state!,
        deviceName,
        platform,
        deviceId,
        max2FAAgeMillis,
      }),
    onSuccess: ({ redirectUrl }) => {
      setStatus("done");
      window.location.assign(redirectUrl);
    },
    onError: (error) => {
      if (error instanceof RsError && error.status === 400) {
        setStatus("invalid");
      } else {
        toast.error(error instanceof Error ? error.message : String(error));
      }
    },
  });

  const cancel = () => {
    setStatus("cancelled");
    window.location.assign(appCancelUrl(state!));
  };

  const question = (
    <div className="flex flex-col gap-4">
      <p data-testid="app-login-question">
        {deviceName
          ? t("mobileApp.login.question", { arg0: deviceName })
          : t("mobileApp.login.questionUnknownDevice")}
      </p>
      {user && (
        <p className="text-sm text-muted-foreground">
          <span data-testid="app-login-user">
            {t("mobileApp.login.loggedInAs", {
              arg0: user.fullname ?? user.username,
            })}
          </span>{" "}
          <Button
            type="button"
            variant="link"
            className="h-auto p-0"
            disabled={authorize.isPending || switchUser.isPending}
            onClick={() => switchUser.mutate()}
          >
            {t("mobileApp.login.otherUser")}
          </Button>
        </p>
      )}
      <Alert>
        <AlertDescription>{t("mobileApp.login.warning")}</AlertDescription>
      </Alert>
      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={authorize.isPending}
          onClick={cancel}
        >
          {t("cancel")}
        </Button>
        <Button
          type="button"
          disabled={authorize.isPending}
          onClick={() => authorize.mutate()}
        >
          {t("mobileApp.login.connect")}
        </Button>
      </div>
    </div>
  );

  return (
    <AuthCard title={t("mobileApp.login.title")}>
      {status === "ask" && isSecondFactorRenewal(params) ? (
        <AppLoginSecondFactor
          maxAgeMillis={max2FAAgeMillis!}
          exemptFallback={question}
          onSuccess={() => authorize.mutate()}
          onCancel={cancel}
        />
      ) : status === "ask" ? (
        question
      ) : (
        <p data-testid="app-login-status">
          {status === "done"
            ? t("mobileApp.login.done")
            : status === "cancelled"
              ? t("mobileApp.login.cancelled")
              : t("mobileApp.login.invalid")}
        </p>
      )}
    </AuthCard>
  );
}
