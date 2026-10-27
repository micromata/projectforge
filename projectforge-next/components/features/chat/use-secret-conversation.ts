"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  sendChatSecret,
  takeChatSecret,
  type ChatSecretSession,
} from "@/lib/rs/chat-secret";
import {
  decryptMessage,
  deriveSessionKey,
  encryptMessage,
  type SecretRole,
} from "@/lib/secret-crypto";

/** One text of the conversation, as shown. Lives in the dialog's state only. */
export interface SecretText {
  key: string;
  mine: boolean;
  counter: number;
  text: string;
}

/**
 * The messages of an open confidential conversation (ChatSecretRelay): derives the session key from the own
 * private and the partner's public key, decrypts what is waiting for this side and encrypts what it sends.
 * Texts are kept in this hook's state only, never in the query cache; a message of the partner is accepted
 * only if its counter rises (a replayed or reordered one is dropped).
 */
export function useSecretConversation(
  role: SecretRole,
  privateKey: CryptoKey | null,
  session: ChatSecretSession | undefined
) {
  const t = useTranslations();
  const [texts, setTexts] = useState<SecretText[]>([]);
  const [aesKey, setAesKey] = useState<CryptoKey | null>(null);
  const received = useRef(0);
  const sent = useRef(0);
  const taking = useRef(false);
  const id = session?.id;
  const partnerKey =
    role === "initiator" ? session?.recipientKey : session?.initiatorKey;
  const partner: SecretRole = role === "initiator" ? "recipient" : "initiator";

  useEffect(() => {
    if (!privateKey || !partnerKey || !id) return;
    let current = true;
    void deriveSessionKey(privateKey, partnerKey, id).then((key) => {
      if (current) setAesKey(key);
    });
    return () => {
      current = false;
    };
  }, [privateKey, partnerKey, id]);

  const take = useMutation({
    mutationFn: async (key: CryptoKey) => {
      const messages = await takeChatSecret(id!);
      const decrypted: SecretText[] = [];
      for (const message of messages) {
        if (message.counter <= received.current) continue;
        try {
          const text = await decryptMessage(message, key, id!, partner);
          received.current = message.counter;
          decrypted.push({
            key: `${partner}-${message.counter}`,
            mine: false,
            counter: message.counter,
            text,
          });
        } catch {
          throw new Error(t("chat.secret.error.decrypt"));
        }
      }
      return decrypted;
    },
    onSuccess: (decrypted) => setTexts((all) => [...all, ...decrypted]),
    onSettled: () => {
      taking.current = false;
    },
    gcTime: 0,
  });
  const takeMutate = take.mutate;
  const waiting = session?.waiting ?? 0;
  useEffect(() => {
    if (!aesKey || waiting === 0 || taking.current) return;
    taking.current = true;
    takeMutate(aesKey);
  }, [aesKey, waiting, takeMutate]);

  const send = useMutation({
    mutationFn: async (text: string) => {
      const counter = sent.current + 1;
      const message = await encryptMessage(text, aesKey!, id!, role, counter);
      await sendChatSecret(id!, message);
      sent.current = counter;
      return { key: `${role}-${counter}`, mine: true, counter, text };
    },
    onSuccess: (sentText) => setTexts((all) => [...all, sentText]),
    // The text is the mutation's variables: not kept in the cache once the dialog is gone.
    gcTime: 0,
  });

  return {
    texts,
    ready: !!aesKey,
    send: send.mutateAsync,
    sending: send.isPending,
    error: take.error ?? send.error,
  };
}
