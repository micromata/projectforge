"use client";

import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import { useSecretFingerprint } from "./use-chat-secret";

/** What a confidential message protects against and what not, shown to both sides. */
export function SecretInfo() {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-1 text-sm text-muted-foreground">
      <p>{t("chat.secret.info")}</p>
      <p>{t("chat.secret.limit")}</p>
    </div>
  );
}

/** The fingerprint of the recipient's key: the same on both sides, unless a key was swapped on the way. */
export function SecretFingerprint({ publicKey }: { publicKey: string | null }) {
  const t = useTranslations();
  const fingerprint = useSecretFingerprint(publicKey);
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
