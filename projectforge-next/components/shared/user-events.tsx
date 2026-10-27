"use client";

import { useEffect } from "react";
import { usePathname } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { OFFERS_MAX_AGE_MS } from "@/hooks/use-chat-secret-offers";
import { useUserEvents } from "@/hooks/use-user-events";
import { CHAT_SECRET_OFFERS_QUERY_KEY } from "@/lib/rs/chat-secret";

/**
 * The server-sent events of the logged-in user, one stream for the whole authenticated area (useUserEvents).
 * Besides, a page change reloads what the events announce if it is a few seconds old, like the notifications do
 * (NotificationToasts): the safety net for a stream that doesn't arrive.
 */
export function UserEvents() {
  useUserEvents();
  const queryClient = useQueryClient();
  const pathname = usePathname();
  useEffect(() => {
    const updatedAt =
      queryClient.getQueryState(CHAT_SECRET_OFFERS_QUERY_KEY)?.dataUpdatedAt ??
      0;
    if (Date.now() - updatedAt > OFFERS_MAX_AGE_MS) {
      void queryClient.invalidateQueries({
        queryKey: CHAT_SECRET_OFFERS_QUERY_KEY,
      });
    }
  }, [pathname, queryClient]);
  return null;
}
