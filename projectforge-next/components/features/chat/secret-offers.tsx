"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { toast } from "@/lib/toast";
import {
  acceptChatSecret,
  cancelChatSecret,
  type ChatSecretSession,
} from "@/lib/rs/chat-secret";
import {
  exportPublicKey,
  generateSecretKeyPair,
  isSecretCryptoSupported,
} from "@/lib/secret-crypto";
import { SecretOfferRow } from "./secret-offer-row";
import { SecretReceiveSteps, type ActiveSecret } from "./secret-receive-dialog";

/**
 * The confidential messages offered to the user in a direct chat: accepting makes the key pair (only
 * here, in memory) and opens the dialog that receives the message (see SecretOfferRow for one accepted
 * elsewhere).
 */
export function SecretOffers({
  sessions,
  onChange,
}: {
  sessions: ChatSecretSession[];
  onChange: () => void;
}) {
  const [active, setActive] = useState<ActiveSecret | null>(null);
  const accept = useMutation({
    mutationFn: async (session: ChatSecretSession) => {
      const keys = await generateSecretKeyPair();
      const publicKey = await exportPublicKey(keys.publicKey);
      await acceptChatSecret(session.id, publicKey);
      return { session, privateKey: keys.privateKey, publicKey };
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
            <SecretReceiveSteps
              active={active}
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
