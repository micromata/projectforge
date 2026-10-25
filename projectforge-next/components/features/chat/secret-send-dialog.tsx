"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { Button } from "@/components/ui/button";
import {
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { deliverChatSecret, offerChatSecret } from "@/lib/rs/chat-secret";
import { encryptSecret, isSecretCryptoSupported } from "@/lib/secret-crypto";
import { SecretSendForm } from "./secret-send-form";
import { SecretFingerprint, SecretInfo, SecretWaiting } from "./secret-parts";
import { useCancelOnUnmount, useSecretSession } from "./use-chat-secret";

/**
 * Sending a confidential message to the partner of a direct chat (ChatSecretRelay): offer, wait for the
 * partner to accept with a key, encrypt for that key, wait until opened. Closing before sending cancels.
 * Rendered inside the dialog, so it starts over each time.
 */
export function SecretSendSteps({
  conversationId,
  partner,
  onClose,
}: {
  conversationId: number;
  partner: string;
  onClose: () => void;
}) {
  const t = useTranslations();
  const [sessionId, setSessionId] = useState<string | null>(null);
  const offer = useMutation({
    mutationFn: () => offerChatSecret(conversationId),
    onSuccess: (session) => setSessionId(session.id),
  });
  const session = useSecretSession(sessionId, true);
  const state = session.data?.state;
  const deliver = useMutation({
    mutationFn: async (text: string) => {
      const data = session.data!;
      const payload = await encryptSecret(text, data.recipientKey!, data.id);
      return deliverChatSecret(data.id, payload);
    },
    onSuccess: () => void session.refetch(),
  });
  const delivered = deliver.isSuccess;
  useCancelOnUnmount(delivered ? null : sessionId);
  const error = offer.error ?? deliver.error;
  let body;
  if (!isSecretCryptoSupported()) {
    body = (
      <FormAlert tone="error">{t("chat.secret.error.unsupported")}</FormAlert>
    );
  } else if (session.isError) {
    body = (
      <FormAlert tone={delivered ? "success" : "info"}>
        {delivered
          ? t("chat.secret.received", { arg0: partner })
          : t("chat.secret.ended")}
      </FormAlert>
    );
  } else if (state === "OFFERED") {
    body = (
      <SecretWaiting
        text={t("chat.secret.waitingForAccept", { arg0: partner })}
      />
    );
  } else if (state === "ACCEPTED") {
    body = (
      <>
        <SecretFingerprint publicKey={session.data?.recipientKey ?? null} />
        <SecretSendForm
          pending={deliver.isPending}
          onSend={(text) => deliver.mutate(text)}
        />
      </>
    );
  } else if (state === "DELIVERED") {
    body = (
      <SecretWaiting text={t("chat.secret.delivered", { arg0: partner })} />
    );
  }
  return (
    <>
      <DialogHeader>
        <DialogTitle>
          {t("chat.secret.sendTitle", { arg0: partner })}
        </DialogTitle>
        <DialogDescription asChild>
          <div>
            <SecretInfo />
          </div>
        </DialogDescription>
      </DialogHeader>
      {body}
      {error && <FormAlert tone="error">{error.message}</FormAlert>}
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          {sessionId && !session.isError && !delivered
            ? t("chat.message.cancel")
            : t("chat.secret.close")}
        </Button>
        {!sessionId && isSecretCryptoSupported() && (
          <Button
            type="button"
            disabled={offer.isPending}
            onClick={() => offer.mutate()}
          >
            {t("chat.secret.start")}
          </Button>
        )}
      </DialogFooter>
    </>
  );
}
