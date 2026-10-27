"use client";

import { useCallback, useEffect, useRef } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  CHAT_SECRET_OFFERS_QUERY_KEY,
  cancelChatSecret,
  chatSecretPendingQueryKey,
  chatSecretQueryKey,
  fetchChatSecret,
  fetchPendingChatSecrets,
} from "@/lib/rs/chat-secret";
import { RsError } from "@/lib/rs/client";
import type { ChatConversation } from "@/lib/rs/chat-types";
import { secretFingerprint } from "@/lib/secret-crypto";

/** The longest confidential text (the server takes at most 16 KB of ciphertext). */
export const SECRET_MAX_LENGTH = 4000;

/** While a side waits for the other one, its dialog asks this often (the event of the chat may not come). */
const POLL_MS = 2000;

/** The conversation is over (404, 403): ended by the other side, expired, or the other side is gone. */
export function isSecretEnded(error: unknown) {
  return (
    error instanceof RsError && (error.status === 404 || error.status === 403)
  );
}

/**
 * A confidential conversation as one of its sides sees it; asked for every [POLL_MS] while [poll] (which
 * also tells the server this side is still there). Only a conversation that ended ([isSecretEnded]) stops
 * the asking: a failed request (network, 5xx) is asked again.
 */
export function useSecretSession(id: string | null, poll: boolean) {
  return useQuery({
    queryKey: chatSecretQueryKey(id ?? ""),
    queryFn: ({ signal }) => fetchChatSecret(id!, signal),
    enabled: !!id,
    refetchInterval: (query) =>
      poll && !isSecretEnded(query.state.error) ? POLL_MS : false,
    refetchOnWindowFocus: false,
    retry: false,
  });
}

/** The fingerprint of both public keys, compared by both sides (see secretFingerprint). */
export function useSecretFingerprint(
  initiatorKey: string | null | undefined,
  recipientKey: string | null | undefined
) {
  return useQuery({
    queryKey: ["chatSecretFingerprint", initiatorKey, recipientKey],
    queryFn: () => secretFingerprint(initiatorKey!, recipientKey!),
    enabled: !!initiatorKey && !!recipientKey,
    staleTime: Infinity,
  }).data;
}

/**
 * Ends the session when the dialog closes (it unmounts with it), unless [id] is null by then: the other
 * side learns at once instead of waiting for it to expire. Deferred by a tick, so the remount of React's
 * strict mode (development) doesn't end it.
 */
export function useCancelOnUnmount(id: string | null) {
  const ref = useRef<string | null>(null);
  const scheduled = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => {
    ref.current = id;
  }, [id]);
  useEffect(() => {
    if (scheduled.current) clearTimeout(scheduled.current);
    scheduled.current = null;
    return () => {
      scheduled.current = setTimeout(() => {
        if (ref.current) void cancelChatSecret(ref.current).catch(() => {});
      }, 0);
    };
  }, []);
}

/**
 * The confidential conversations of a direct chat the user is the recipient of, and `refresh` for the event of
 * the chat (useChatLive's onSecret), which also refreshes an open session of either side.
 */
export function useSecretOffers(conversation: ChatConversation | undefined) {
  const queryClient = useQueryClient();
  const conversationId = conversation?.id ?? 0;
  const query = useQuery({
    queryKey: chatSecretPendingQueryKey(conversationId),
    queryFn: ({ signal }) => fetchPendingChatSecrets(conversationId, signal),
    enabled: !!conversation?.directPartner,
    refetchOnWindowFocus: false,
  });
  const direct = !!conversation?.directPartner;
  const refresh = useCallback(() => {
    if (!direct) return;
    void queryClient.invalidateQueries({
      queryKey: chatSecretPendingQueryKey(conversationId),
    });
    void queryClient.invalidateQueries({ queryKey: ["chatSecret"] });
    // The counter in the top bar and the signals follow the answer, with the stream or without it.
    void queryClient.invalidateQueries({
      queryKey: CHAT_SECRET_OFFERS_QUERY_KEY,
    });
  }, [direct, conversationId, queryClient]);
  return { sessions: query.data ?? [], refresh };
}
