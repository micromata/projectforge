"use client";

import { useEffect, useRef, useState } from "react";
import { useChatMessages } from "./use-chat-messages";
import { useUnreadMessages } from "./use-unread-messages";

/**
 * The unread messages not seen yet ([useUnreadMessages]) and the jump to the next one: a reply opens its thread
 * at it, a message of the conversation is scrolled to, loading older messages until it is there (one not shown,
 * e.g. hidden meanwhile, is skipped). [jumpedId] is the message jumped to last, to be highlighted.
 */
export function useUnreadJump(
  conversationId: number,
  readMark: number | null,
  marks: number,
  openThread: (rootId: number, focusId?: number) => void,
  closeThread: () => void
) {
  const unread = useUnreadMessages(conversationId, readMark, marks);
  const { query, messages } = useChatMessages(conversationId);
  // The message of the conversation to scroll to once loaded.
  const target = useRef<number | null>(null);
  const [jumpedId, setJumpedId] = useState<number | null>(null);
  const { hasNextPage, isFetchingNextPage, fetchNextPage } = query;
  const { markSeen } = unread;

  /** Scrolls to the target if loaded, loads older messages otherwise; skips it if there are none. */
  const scrollToTarget = () => {
    const id = target.current;
    if (id == null) return;
    const element = messages.some((it) => it.id === id)
      ? document.getElementById(`chat-message-${id}`)
      : null;
    if (element) {
      target.current = null;
      element.scrollIntoView({ block: "center" });
    } else if (hasNextPage) {
      if (!isFetchingNextPage) void fetchNextPage();
    } else {
      target.current = null;
      markSeen(id);
    }
  };
  const scrollRef = useRef(scrollToTarget);
  useEffect(() => {
    scrollRef.current = scrollToTarget;
  });
  // After rendering (the list may follow its end on a change, see MessageList), and as older messages came in.
  useEffect(
    () => scrollRef.current(),
    [messages, isFetchingNextPage, jumpedId]
  );

  const jump = () => {
    const next = unread.pending[0];
    if (!next) return;
    setJumpedId(next.id);
    if (next.threadRootId != null) {
      openThread(next.threadRootId, next.id);
      return;
    }
    closeThread();
    target.current = next.id;
    if (next.id === jumpedId) scrollToTarget(); // Not rendered again.
  };

  return {
    count: unread.pending.length,
    jump,
    jumpedId,
    observe: unread.observe,
    refresh: unread.refresh,
  };
}
