"use client";

import { useQuery } from "@tanstack/react-query";
import { chatPartnerReadQueryKey, fetchChatPartnerRead } from "@/lib/rs/chat";
import { useChatContext } from "./chat-context";

/**
 * How far the other one of a direct message has read: the id of the newest message, null for none (or in any
 * other conversation). Refreshed by the live updates of the conversation (see ChatPanel).
 */
export function usePartnerRead(): number | null {
  const { conversation } = useChatContext();
  const query = useQuery({
    queryKey: chatPartnerReadQueryKey(conversation.id),
    queryFn: ({ signal }) => fetchChatPartnerRead(conversation.id, signal),
    enabled: conversation.directPartner != null,
    // Kept current by the live updates (see ChatPanel), not by every focus or remount.
    staleTime: 60_000,
    refetchOnWindowFocus: false,
  });
  return query.data?.lastReadMessageId ?? null;
}
