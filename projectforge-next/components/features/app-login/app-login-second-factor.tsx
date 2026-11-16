"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { fetchTwoFactorState } from "@/lib/rs/auth";
import { FormAlert } from "@/components/shared/form-alert";
import { TwoFactorForm } from "@/components/shared/two-factor-form";

/**
 * The app renews its login for the second factor a page of its WebView asked for (isSecondFactorRenewal): instead
 * of the question "connect the app" the second factor itself, WebAuthn started right away. Its success is the
 * user's confirmation, and it is always a new one, so the app gets a second factor as fresh as the page needs.
 */
export function AppLoginSecondFactor({
  deviceName,
  maxAgeMillis,
  exemptFallback,
  onSuccess,
  onCancel,
}: {
  deviceName: string | null;
  maxAgeMillis: number;
  /** Shown instead for a user exempt from the second factor (app review): there is none to confirm with. */
  exemptFallback: React.ReactNode;
  onSuccess: () => void;
  onCancel: () => void;
}) {
  const t = useTranslations();
  const state = useQuery({
    queryKey: ["appLogin", "twoFactorState", maxAgeMillis],
    queryFn: () => fetchTwoFactorState(maxAgeMillis),
    // A refetch would only replace the methods while the user enters a code.
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });

  if (state.isError) {
    return (
      <FormAlert tone="error">{t("user.My2FACode.error.validation")}</FormAlert>
    );
  }
  if (!state.data) {
    return null;
  }
  if (state.data.exempt) {
    return exemptFallback;
  }
  return (
    <TwoFactorForm
      context="session"
      methods={state.data.methods}
      hint={
        deviceName
          ? t("mobileApp.login.secondFactor", { arg0: deviceName })
          : t("mobileApp.login.secondFactorUnknownDevice")
      }
      expiryMillis={maxAgeMillis}
      autoStartWebAuthn
      onSuccess={onSuccess}
      onCancel={onCancel}
    />
  );
}
