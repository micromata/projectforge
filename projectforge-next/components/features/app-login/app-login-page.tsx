"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { AuthCard } from "@/components/shared/auth-card";
import { RsError } from "@/lib/rs/client";
import { appCancelUrl, authorizeApp } from "@/lib/rs/app-auth";
import { toast } from "@/lib/toast";
import { readAppLoginParams } from "./app-login-params";

type Status = "ask" | "done" | "cancelled" | "invalid";

/**
 * Opened by the ProjectForge app in the system browser (`/appLogin?login=…&challenge=…&state=…`, see
 * readAppLoginParams): after the normal login (AuthGuard) the user confirms the app, the server demands a
 * fresh second factor, and the browser hands the one-time code to the app via its callback url
 * (AppAuthService). The explicit
 * confirmation keeps a link sent by somebody else from connecting an app unnoticed.
 */
export function AppLoginPage() {
  const t = useTranslations();
  const { challenge, state, deviceName, platform, deviceId } =
    readAppLoginParams(useSearchParams());
  const [status, setStatus] = useState<Status>(
    challenge && state ? "ask" : "invalid"
  );

  const authorize = useMutation({
    mutationFn: () =>
      authorizeApp({
        challenge: challenge!,
        state: state!,
        deviceName,
        platform,
        deviceId,
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

  return (
    <AuthCard title={t("mobileApp.login.title")}>
      {status === "ask" ? (
        <div className="flex flex-col gap-4">
          <p data-testid="app-login-question">
            {deviceName
              ? t("mobileApp.login.question", { arg0: deviceName })
              : t("mobileApp.login.questionUnknownDevice")}
          </p>
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
