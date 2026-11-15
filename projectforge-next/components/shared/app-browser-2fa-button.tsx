"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { confirm2FAInApp } from "@/lib/app-bridge";

interface AppBrowser2FAButtonProps {
  /** The second factor must be fresher than this (`expiryMillis` of the refused request). */
  maxAgeMillis?: number | null;
  onSuccess: () => void;
  onError: (message: string) => void;
}

/**
 * In the app instead of WebAuthn, which its WebView can't do: the app confirms the second factor in the system
 * browser (WebAuthn, OTP, …) and brings a new web session (see confirm2FAInApp).
 */
export function AppBrowser2FAButton({
  maxAgeMillis,
  onSuccess,
  onError,
}: AppBrowser2FAButtonProps) {
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const tb = useTranslations();
  const [isWaiting, setIsWaiting] = useState(false);

  async function confirm() {
    setIsWaiting(true);
    try {
      if (await confirm2FAInApp(maxAgeMillis)) {
        onSuccess();
      } else {
        onError(tb("user.My2FACode.confirmInBrowser.failed"));
      }
    } finally {
      setIsWaiting(false);
    }
  }

  return (
    <Button
      type="button"
      variant="outline"
      disabled={isWaiting}
      onClick={confirm}
    >
      {tb("user.My2FACode.confirmInBrowser._")}
    </Button>
  );
}
