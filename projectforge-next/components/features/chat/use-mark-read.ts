"use client";

import { useCallback, useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { markChatRead } from "@/lib/rs/chat";

const DELAY_MS = 1_000;

/**
 * Marks the conversation read up to the newest message known (NOT-01) while it is shown at its end in a visible
 * tab, a second after the last change; the notifications about it are resolved by the server, so the badges of
 * the menu and the notifications are refreshed. Marked unread, it pauses until resumed.
 */
export function useMarkRead(conversationId: number) {
  const queryClient = useQueryClient();
  const marked = useRef(0);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pending = useRef(0);
  const paused = useRef(false);
  // The read state sent last: marking unread waits for it, so it can't arrive later and undo it.
  const inFlight = useRef<Promise<unknown>>(Promise.resolve());

  const flush = useCallback(() => {
    const id = pending.current;
    if (paused.current || id <= marked.current || document.hidden) return;
    marked.current = id;
    const request = markChatRead(conversationId, id);
    inFlight.current = request.catch(() => undefined);
    request.then(
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
      if (paused.current || !messageId || messageId <= marked.current) return;
      pending.current = Math.max(pending.current, messageId);
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(flush, DELAY_MS);
    },
    [flush]
  );

  /** Marked unread, read up to [readUpTo]: doesn't mark read until [resume]. */
  const pause = useCallback((readUpTo: number) => {
    paused.current = true;
    if (timer.current) clearTimeout(timer.current);
    marked.current = readUpTo;
    pending.current = readUpTo;
  }, []);

  const resume = useCallback(() => {
    paused.current = false;
  }, []);

  /** Resolved when the read state sent last arrived (or failed). */
  const settled = useCallback(() => inFlight.current, []);

  useEffect(() => {
    const onVisibility = () => !document.hidden && flush();
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      if (timer.current) clearTimeout(timer.current);
    };
  }, [flush]);

  return { markUpTo, pause, resume, settled };
}
