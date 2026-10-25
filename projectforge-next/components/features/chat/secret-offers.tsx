"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
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
import { SecretReceiveSteps, type ActiveSecret } from "./secret-receive-dialog";

/**
 * The confidential messages offered to the user in a direct chat: accepting makes the key pair (only
 * here, in memory) and opens the dialog that receives the message. One the user can't open anymore (its
 * key is gone with a reload) can only be discarded.
 */
export function SecretOffers({
  sessions,
  onChange,
}: {
  sessions: ChatSecretSession[];
  onChange: () => void;
}) {
  const t = useTranslations();
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
  });
  const discard = (session: ChatSecretSession) =>
    void cancelChatSecret(session.id).finally(onChange);
  const shown = sessions.filter((it) => it.id !== active?.session.id);
  return (
    <>
      {shown.map((session) => {
        const sender = session.sender.displayName;
        const offered = session.state === "OFFERED";
        return (
          <div
            key={session.id}
            className="flex shrink-0 flex-wrap items-center gap-2 border-t bg-primary/5 px-3 py-2 text-sm"
            data-testid="chat-secret-offer"
          >
            <p className="flex-1">
              {offered
                ? t("chat.secret.offered", { arg0: sender })
                : t("chat.secret.lost", { arg0: sender })}
            </p>
            <Button
              type="button"
              size="sm"
              variant="outline"
              onClick={() => discard(session)}
            >
              {offered ? t("chat.secret.decline") : t("chat.secret.discard")}
            </Button>
            {offered && (
              <Button
                type="button"
                size="sm"
                disabled={accept.isPending || !isSecretCryptoSupported()}
                onClick={() => accept.mutate(session)}
              >
                {t("chat.secret.accept")}
              </Button>
            )}
          </div>
        );
      })}
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
