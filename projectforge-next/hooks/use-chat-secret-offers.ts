"use client";

import { useQuery } from "@tanstack/react-query";
import {
  CHAT_SECRET_OFFERS_QUERY_KEY,
  fetchChatSecretOffers,
  type ChatSecretSession,
} from "@/lib/rs/chat-secret";

/** Younger than this, a page change or focus doesn't reload the offers. */
export const OFFERS_MAX_AGE_MS = 5_000;

const NONE: ChatSecretSession[] = [];

/**
 * The confidential conversations offered to the user and waiting for an answer, in all direct chats. Not polled:
 * the user's event stream (useUserEvents) invalidates it when an offer changes, so the top bar's counter and the
 * attention signals (SecretOfferAlerts) share this one query. Should the stream not arrive (a proxy buffering it,
 * too many open connections of the browser), a page change and the focus of the window reload it as well.
 */
export function useChatSecretOffers(): ChatSecretSession[] {
  const { data } = useQuery({
    queryKey: CHAT_SECRET_OFFERS_QUERY_KEY,
    queryFn: ({ signal }) => fetchChatSecretOffers(signal),
    staleTime: OFFERS_MAX_AGE_MS,
    refetchOnWindowFocus: true,
    retry: false,
  });
  return data ?? NONE;
}
