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
  // Resolves the pending requests' promises: true = retry, false = give up.
  // Several requests may be interrupted by the same expiry, so this is a list.
  const resolversRef = useRef<((success: boolean) => void)[]>([]);

  const settle = useCallback((success: boolean) => {
    const resolvers = resolversRef.current;
    resolversRef.current = [];
    resolvers.forEach((resolve) => resolve(success));
    setIsOpen(false);
    setState(null);
    setLoadError(null);
  }, []);

  useEffect(() => {
    setTwoFactorHandler((expiryMillis) => {
      // A second demand while the dialog is open joins the pending one: one
      // successful 2FA lets every interrupted request through.
      const pending = new Promise<boolean>((resolve) => {
        resolversRef.current.push(resolve);
      });
      if (resolversRef.current.length === 1) {
        setIsOpen(true);
        fetchTwoFactorState(expiryMillis)
          .then(setState)
          .catch(() => setLoadError(t("user.My2FACode.error.validation")));
      }
      return pending;
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
