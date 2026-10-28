"use client";

import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { markChatThreadRead } from "@/lib/rs/chat";
import type { ChatMessage } from "@/lib/rs/chat-types";

/**
 * Resolves the notifications about mentions of the user in the open thread, shown in a visible tab (reading the
 * conversation doesn't resolve them). Asked only for a mention not resolved before; the badges are refreshed.
 */
export function useMarkThreadRead(
  conversationId: number,
  rootId: number | null,
  replies: ChatMessage[] | undefined
) {
  const queryClient = useQueryClient();
  const resolved = useRef(new Map<number, number>());
  const newestMention = Math.max(
    0,
    ...(replies ?? []).filter((it) => it.mentionsMe).map((it) => it.id)
  );

  useEffect(() => {
    if (rootId == null || newestMention === 0) return;
    const resolve = () => {
      if (document.hidden) return;
      if ((resolved.current.get(rootId) ?? 0) >= newestMention) return;
      resolved.current.set(rootId, newestMention);
      markChatThreadRead(conversationId, rootId).then(
        () => {
          void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
          void queryClient.invalidateQueries({ queryKey: ["menu"] });
        },
        () => resolved.current.delete(rootId) // Tried again with the next change.
      );
    };
    resolve();
    document.addEventListener("visibilitychange", resolve);
    return () => document.removeEventListener("visibilitychange", resolve);
  }, [conversationId, rootId, newestMention, queryClient]);
}
