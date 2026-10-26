"use client";

import { useMemo } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchChatMessages } from "@/lib/rs/chat";
import { chatMessagesQueryKey } from "./chat-cache";

/**
 * The messages of the conversation (not of threads), loaded in pages of 50 from the newest one (MSG-06) and
 * kept up to date by [useChatLive], which merges the changes into this cache: no refetching.
 * [messages] are the loaded ones in chronological order.
 */
export function useChatMessages(conversationId: number) {
  const query = useInfiniteQuery({
    queryKey: chatMessagesQueryKey(conversationId),
    queryFn: ({ pageParam, signal }) =>
      fetchChatMessages(conversationId, pageParam, signal),
    initialPageParam: null as number | null,
    getNextPageParam: (last) =>
      last.hasMore
        ? (last.messages[last.messages.length - 1]?.id ?? undefined)
        : undefined,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  const messages = useMemo(
    () =>
      (query.data?.pages.flatMap((page) => page.messages) ?? [])
        .slice()
        .reverse(),
    [query.data]
  );
  return { query, messages };
}
