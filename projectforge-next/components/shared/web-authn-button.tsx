"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FingerPrintIcon } from "@hugeicons/core-free-icons";
import {
  fetchWebAuthnOptions,
  finishWebAuthn,
  type TwoFactorContext,
  type TwoFactorResult,
} from "@/lib/rs/auth";
import { authenticate, isWebAuthnSupported } from "@/lib/webauthn";
import { Button } from "@/components/ui/button";

interface WebAuthnButtonProps {
  context: TwoFactorContext;
  /**
   * Start the ceremony right after mount. The legacy app does this in the login
   * context, where a token is the fastest path and no other input is expected.
   */
  autoStart?: boolean;
  onSuccess: (result: TwoFactorResult) => void;
  onError: (message: string) => void;
}

export function WebAuthnButton({
  context,
  autoStart = false,
  onSuccess,
  onError,
}: WebAuthnButtonProps) {
  const t = useTranslations("login.twoFactor");
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const [isRunning, setIsRunning] = useState(false);
  // A second ceremony while the first prompt is open aborts the first one.
  const started = useRef(false);

  const run = useCallback(async () => {
    if (started.current) return;
    started.current = true;
    setIsRunning(true);
    try {
      const options = await fetchWebAuthnOptions(context);
      const finishRequest = await authenticate(options);
      if (!finishRequest) {
        return; // The user dismissed the prompt — not an error.
      }
      const result = await finishWebAuthn(context, finishRequest);
      if (result.success) {
        onSuccess(result);
      } else {
        onError(result.message ?? tb("user.My2FACode.error.validation"));
      }
    } catch {
      onError(tb("webauthn.error.validate"));
    } finally {
      started.current = false;
      setIsRunning(false);
    }
  }, [context, onError, onSuccess, tb]);

  useEffect(() => {
    if (autoStart && isWebAuthnSupported()) {
      void run();
    }
  }, [autoStart, run]);

  if (!isWebAuthnSupported()) {
    return null;
  }

  return (
    <Button type="button" variant="outline" onClick={run} disabled={isRunning}>
      <HugeiconsIcon icon={FingerPrintIcon} className="size-4" />
      {isRunning
        ? t("webAuthnRunning")
        : tb("webauthn.registration.button.authenticate._")}
    </Button>
  );
}
