"use client";

import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import {
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useSecretFingerprint } from "./use-chat-secret";

/** What a confidential conversation protects against and what not, shown to both sides. */
export function SecretInfo() {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-1 text-sm text-muted-foreground">
      <p>{t("chat.secret.info")}</p>
      <p>{t("chat.secret.limit")}</p>
    </div>
  );
}

/** The fingerprint of both keys: the same on both sides, unless a key was swapped on the way. */
export function SecretFingerprint({
  initiatorKey,
  recipientKey,
}: {
  initiatorKey: string | null;
  recipientKey: string | null;
}) {
  const t = useTranslations();
  const fingerprint = useSecretFingerprint(initiatorKey, recipientKey);
  if (!fingerprint) return null;
  return (
    <p className="text-sm" data-testid="chat-secret-fingerprint">
      {t("chat.secret.fingerprint")}:{" "}
      <span className="font-mono tracking-wider">{fingerprint}</span>
    </p>
  );
}

/** A step waiting for the other side. */
export function SecretWaiting({ text }: { text: string }) {
  return (
    <p className="flex items-center gap-2 text-sm" aria-busy>
      <Spinner className="h-4 w-4 border-2" />
      {text}
    </p>
  );
}

/** Title and the notes about what a confidential conversation protects against. */
export function SecretHeader({ title }: { title: string }) {
  return (
    <DialogHeader>
      <DialogTitle>{title}</DialogTitle>
      <DialogDescription asChild>
        <div>
          <SecretInfo />
        </div>
      </DialogDescription>
    </DialogHeader>
  );
}
