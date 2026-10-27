"use client";

import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { toast } from "@/lib/toast";
import {
  acceptChatSecret,
  cancelChatSecret,
  markChatSecretsSeen,
  type ChatSecretSession,
} from "@/lib/rs/chat-secret";
import {
  exportPublicKey,
  generateSecretKeyPair,
  isSecretCryptoSupported,
} from "@/lib/secret-crypto";
import { SecretOfferRow } from "./secret-offer-row";
import { SecretConversationSteps } from "./secret-conversation-dialog";

/** An accepted confidential conversation with the recipient's private key (in memory only). */
interface ActiveSecret {
  session: ChatSecretSession;
  privateKey: CryptoKey;
}

/**
 * The confidential conversations offered to the user in a direct chat: accepting makes the key pair (only
 * here, in memory) and opens the conversation dialog (see SecretOfferRow for one accepted elsewhere). Shown here, their notification is resolved: its banner would only point to where the user
 * already is.
 */
export function SecretOffers({
  sessions,
  onChange,
}: {
  sessions: ChatSecretSession[];
  onChange: () => void;
}) {
  const [active, setActive] = useState<ActiveSecret | null>(null);
  useResolveNotificationOnSight(sessions);
  const accept = useMutation({
    mutationFn: async (session: ChatSecretSession) => {
      const keys = await generateSecretKeyPair();
      const publicKey = await exportPublicKey(keys.publicKey);
      await acceptChatSecret(session.id, publicKey);
      return { session, privateKey: keys.privateKey };
    },
    onSuccess: setActive,
    onError: (error: Error) => toast.error(error.message),
    onSettled: onChange,
    // The private key is in the mutation's data: only in this component's state, not in the cache.
    gcTime: 0,
  });
  const discard = (session: ChatSecretSession) =>
    void cancelChatSecret(session.id).finally(onChange);
  const shown = sessions.filter((it) => it.id !== active?.session.id);
  return (
    <>
      {shown.map((session) => (
        <SecretOfferRow
          key={session.id}
          session={session}
          accepting={accept.isPending}
          canAccept={isSecretCryptoSupported()}
          onAccept={() => accept.mutate(session)}
          onDiscard={() => discard(session)}
        />
      ))}
      <Dialog open={!!active} onOpenChange={(open) => !open && setActive(null)}>
        <DialogContent className="sm:max-w-md">
          {active && (
            <SecretConversationSteps
              role="recipient"
              partner={active.session.initiator.displayName}
              sessionId={active.session.id}
              privateKey={active.privateKey}
              onClose={() => {
                setActive(null);
                onChange();
              }}
            />
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}

/** Resolves the notification of the offers once they are shown (by the server, for the conversation). */
function useResolveNotificationOnSight(sessions: ChatSecretSession[]) {
  const queryClient = useQueryClient();
  const offered = sessions.filter((it) => it.state === "OFFERED");
  const conversationId = offered[0]?.conversationId;
  // Changes with a new offer only, so each one is resolved once.
  const key = offered.map((it) => it.id).join(",");
  useEffect(() => {
    if (!key || conversationId == null) return;
    void markChatSecretsSeen(conversationId)
      .then(() => {
        void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
        void queryClient.invalidateQueries({ queryKey: ["notifications"] });
        void queryClient.invalidateQueries({ queryKey: ["menu"] });
      })
      .catch(() => {});
  }, [key, conversationId, queryClient]);
}
