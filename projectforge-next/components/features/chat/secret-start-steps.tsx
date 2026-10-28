"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { Button } from "@/components/ui/button";
import { DialogFooter } from "@/components/ui/dialog";
import { offerChatSecret } from "@/lib/rs/chat-secret";
import {
  exportPublicKey,
  generateSecretKeyPair,
  isSecretCryptoSupported,
} from "@/lib/secret-crypto";
import { SecretConversationSteps } from "./secret-conversation-dialog";
import { SecretHeader } from "./secret-parts";

/**
 * Starting a confidential conversation with the partner of a direct chat: makes the key pair (only here,
 * in memory), offers the session with the public key and goes on in the conversation. Rendered inside the
 * dialog, so it starts over each time.
 */
export function SecretStartSteps({
  conversationId,
  partner,
  onClose,
}: {
  conversationId: number;
  partner: string;
  onClose: () => void;
}) {
  const t = useTranslations();
  const [started, setStarted] = useState<{
    sessionId: string;
    privateKey: CryptoKey;
  } | null>(null);
  const offer = useMutation({
    mutationFn: async () => {
      const keys = await generateSecretKeyPair();
      const session = await offerChatSecret(
        conversationId,
        await exportPublicKey(keys.publicKey)
      );
      return { sessionId: session.id, privateKey: keys.privateKey };
    },
    onSuccess: setStarted,
    // The private key is in the mutation's data: only in this component's state, not in the cache.
    gcTime: 0,
  });
  if (started) {
    return (
      <SecretConversationSteps
        role="initiator"
        partner={partner}
        sessionId={started.sessionId}
        privateKey={started.privateKey}
        onClose={onClose}
      />
    );
  }
  const supported = isSecretCryptoSupported();
  return (
    <>
      <SecretHeader title={t("chat.secret.sendTitle", { arg0: partner })} />
      {!supported && (
        <FormAlert tone="error">{t("chat.secret.error.unsupported")}</FormAlert>
      )}
      {offer.error && <FormAlert tone="error">{offer.error.message}</FormAlert>}
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          {t("chat.secret.close")}
        </Button>
        {supported && (
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
