"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { fetchTwoFactorState, type TwoFactorState } from "@/lib/rs/auth";
import { setTwoFactorHandler } from "@/lib/rs/client";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { FormAlert } from "@/components/shared/form-alert";
import { TwoFactorForm } from "@/components/shared/two-factor-form";

/**
 * Shows the second-factor dialog whenever the backend demands one for a
 * protected action (My2FARequestHandler). The interrupted request is repeated
 * by lib/rs/client.ts as soon as this resolves to true.
 */
export function TwoFactorProvider({ children }: { children: React.ReactNode }) {
  // Backend bundle keys (GenerateNextI18nMessagesMain): shared with Wicket/React.
  const t = useTranslations();
  const [state, setState] = useState<TwoFactorState | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  // Resolves the pending request()'s promise: true = retry, false = give up.
  const resolveRef = useRef<((success: boolean) => void) | null>(null);

  const settle = useCallback((success: boolean) => {
    resolveRef.current?.(success);
    resolveRef.current = null;
    setIsOpen(false);
    setState(null);
    setLoadError(null);
  }, []);

  useEffect(() => {
    setTwoFactorHandler((expiryMillis) => {
      // A second demand while the dialog is open joins the pending one.
      if (resolveRef.current) return Promise.resolve(false);
      setIsOpen(true);
      fetchTwoFactorState(expiryMillis)
        .then(setState)
        .catch(() => setLoadError(t("user.My2FACode.error.validation")));
      return new Promise<boolean>((resolve) => {
        resolveRef.current = resolve;
      });
    });
    return () => setTwoFactorHandler(null);
  }, [t]);

  return (
    <>
      {children}
      <Dialog
        open={isOpen}
        onOpenChange={(open) => {
          if (!open) settle(false);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t("user.My2FACode.title")}</DialogTitle>
            <DialogDescription className="sr-only">
              {t("user.My2FACode.authentification.info")}
            </DialogDescription>
          </DialogHeader>
          {loadError && <FormAlert tone="error">{loadError}</FormAlert>}
          {state && (
            <TwoFactorForm
              context="session"
              methods={state.methods}
              hint={state.expiryMessage}
              onSuccess={() => settle(true)}
              onCancel={() => settle(false)}
            />
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
