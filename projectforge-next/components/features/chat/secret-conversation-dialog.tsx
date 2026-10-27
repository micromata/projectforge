"use client";

import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import { Button } from "@/components/ui/button";
import { DialogFooter } from "@/components/ui/dialog";
import type { SecretRole } from "@/lib/secret-crypto";
import { SecretMessageList } from "./secret-message-list";
import { SecretFingerprint, SecretHeader, SecretWaiting } from "./secret-parts";
import { SecretSendForm } from "./secret-send-form";
import { useCancelOnUnmount, useSecretSession } from "./use-chat-secret";
import { useSecretConversation } from "./use-secret-conversation";

/**
 * A confidential conversation (ChatSecretRelay) from the offer on: waiting for the partner's key, then
 * both write until one side ends it. Closing the dialog ends it. Keys and texts live in this component
 * only (the private key is not extractable).
 */
export function SecretConversationSteps({
  role,
  partner,
  sessionId,
  privateKey,
  onClose,
}: {
  role: SecretRole;
  partner: string;
  sessionId: string;
  privateKey: CryptoKey;
  onClose: () => void;
}) {
  const t = useTranslations();
  const session = useSecretSession(sessionId, true);
  const ended = session.isError;
  const conversation = useSecretConversation(role, privateKey, session.data);
  useCancelOnUnmount(sessionId);
  const open = session.data?.state === "OPEN";
  const title =
    role === "initiator"
      ? t("chat.secret.sendTitle", { arg0: partner })
      : t("chat.secret.receiveTitle", { arg0: partner });
  const error = conversation.error;
  return (
    <>
      <SecretHeader title={title} />
      {open && (
        <SecretFingerprint
          initiatorKey={session.data?.initiatorKey ?? null}
          recipientKey={session.data?.recipientKey ?? null}
        />
      )}
      {!ended && !open && (
        <SecretWaiting
          text={
            role === "initiator"
              ? t("chat.secret.waitingForAccept", { arg0: partner })
              : t("chat.secret.waitingForSender", { arg0: partner })
          }
        />
      )}
      {(open || conversation.texts.length > 0) && (
        <SecretMessageList
          texts={conversation.texts}
          readCounter={session.data?.readCounter ?? 0}
          partner={partner}
        />
      )}
      {ended && <FormAlert tone="info">{t("chat.secret.ended")}</FormAlert>}
      {error && <FormAlert tone="error">{error.message}</FormAlert>}
      {open && !ended && (
        <SecretSendForm
          pending={conversation.sending || !conversation.ready}
          onSend={conversation.send}
        />
      )}
      <DialogFooter>
        <Button type="button" variant="outline" onClick={onClose}>
          {ended ? t("chat.secret.close") : t("chat.secret.end")}
        </Button>
      </DialogFooter>
    </>
  );
}
