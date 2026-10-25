"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { CopyButton } from "@/components/shared/copy-button";
import { FormAlert } from "@/components/shared/form-alert";
import { Button } from "@/components/ui/button";
import {
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { takeChatSecret, type ChatSecretSession } from "@/lib/rs/chat-secret";
import { decryptSecret } from "@/lib/secret-crypto";
import { SecretFingerprint, SecretInfo, SecretWaiting } from "./secret-parts";
import { useCancelOnUnmount, useSecretSession } from "./use-chat-secret";

/** An accepted confidential message with the recipient's keys (in memory only). */
export interface ActiveSecret {
  session: ChatSecretSession;
  privateKey: CryptoKey;
  publicKey: string;
}

/**
 * Receiving a confidential message: wait for the sender, take the ciphertext (only once, the server deletes
 * it) and decrypt it here. The text lives in this component only; closing drops it and cancels a session
 * not yet received.
 */
export function SecretReceiveSteps({
  active,
  onClose,
}: {
  active: ActiveSecret;
  onClose: () => void;
}) {
  const t = useTranslations();
  const id = active.session.id;
  const sender = active.session.sender.displayName;
  const take = useMutation({
    mutationFn: async () => {
      const payload = await takeChatSecret(id);
      try {
        return await decryptSecret(payload, active.privateKey, id);
      } catch {
        throw new Error(t("chat.secret.error.decrypt"));
      }
    },
    // The decrypted text is the mutation's data: not kept in the cache once the dialog is gone.
    gcTime: 0,
  });
  const session = useSecretSession(take.isIdle ? id : null, true);
  useCancelOnUnmount(take.isIdle ? id : null);
  let body;
  if (take.data != null) {
    body = (
      <div className="flex flex-col gap-1.5">
        <div className="flex items-start gap-2">
          <Textarea
            readOnly
            value={take.data}
            aria-label={t("chat.secret.text")}
            className="min-h-24 font-mono"
            data-testid="chat-secret-text"
          />
          <CopyButton value={take.data} label={t("chat.secret.text")} />
        </div>
        <p className="text-sm text-muted-foreground">
          {t("chat.secret.shownOnce")}
        </p>
      </div>
    );
  } else if (take.isError) {
    body = <FormAlert tone="error">{take.error.message}</FormAlert>;
  } else if (session.isError) {
    body = <FormAlert tone="info">{t("chat.secret.ended")}</FormAlert>;
  } else if (session.data?.state === "DELIVERED") {
    body = (
      <Button
        type="button"
        className="self-start"
        disabled={take.isPending}
        onClick={() => take.mutate()}
      >
        {t("chat.secret.show")}
      </Button>
    );
  } else {
    body = (
      <SecretWaiting
        text={t("chat.secret.waitingForSender", { arg0: sender })}
      />
    );
  }
  return (
    <>
      <DialogHeader>
        <DialogTitle>
          {t("chat.secret.receiveTitle", { arg0: sender })}
        </DialogTitle>
        <DialogDescription asChild>
          <div>
            <SecretInfo />
          </div>
        </DialogDescription>
      </DialogHeader>
      {take.isIdle && <SecretFingerprint publicKey={active.publicKey} />}
      {body}
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          {take.isIdle && !session.isError
            ? t("chat.message.cancel")
            : t("chat.secret.close")}
        </Button>
      </DialogFooter>
    </>
  );
}
