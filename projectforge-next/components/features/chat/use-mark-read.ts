"use client";

import { useCallback, useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { markChatRead } from "@/lib/rs/chat";

const DELAY_MS = 1_000;

/**
 * Marks the conversation read up to the newest message known (NOT-01) while it is shown at its end in a visible
 * tab, a second after the last change; the notifications about it are resolved by the server, so the badges of
 * the menu and the notifications are refreshed.
 */
export function useMarkRead(conversationId: number) {
  const queryClient = useQueryClient();
  const marked = useRef(0);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pending = useRef(0);

  const flush = useCallback(() => {
    const id = pending.current;
    if (id <= marked.current || document.hidden) return;
    marked.current = id;
    markChatRead(conversationId, id).then(
      () => {
        void queryClient.invalidateQueries({ queryKey: ["userStatus"] });
        void queryClient.invalidateQueries({ queryKey: ["menu"] });
        void queryClient.invalidateQueries({ queryKey: ["chatRooms"] });
        void queryClient.invalidateQueries({ queryKey: ["chatDirect"] });
      },
      () => {
        marked.current = 0; // Tried again with the next change.
      }
    );
  }, [conversationId, queryClient]);

  /** The newest message id is shown (at the end of the list): mark it read soon. */
  const markUpTo = useCallback(
    (messageId: number | null | undefined) => {
      if (!messageId || messageId <= marked.current) return;
      pending.current = Math.max(pending.current, messageId);
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(flush, DELAY_MS);
    },
    [flush]
  );

  useEffect(() => {
    const onVisibility = () => !document.hidden && flush();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      if (timer.current) clearTimeout(timer.current);
    };
  }, [flush]);

  return markUpTo;
}
