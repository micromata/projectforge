"use client";

import { useCallback, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { markChatUnread } from "@/lib/rs/chat";
import type { ChatConversation, ChatMessage } from "@/lib/rs/chat-types";
import { invalidateUnreadCounts } from "./chat-cache";

/**
 * The read state when the conversation was opened ([readMark], for the "new" lines and the unread messages): marking
 * read meanwhile doesn't move it, marking unread ([markUnread]) does. [marks] counts the markings, as marking unread
 * may come back to the same read state, seen anew all the same. The threads opened since ([threadOpened]) have no
 * unread replies any more.
 */
export function useReadMark(
  conversationId: number,
  conversation: ChatConversation | undefined,
  pause: (id: number) => void,
  settled: () => Promise<unknown>
) {
  const queryClient = useQueryClient();
  const [readMark, setReadMark] = useState<number | null | undefined>(
    undefined
  );
  // Never read: everything of others is new, as counted by the server.
  if (conversation && readMark === undefined)
    setReadMark(conversation.lastReadMessageId ?? 0);
  const [marks, setMarks] = useState(0);
  const [openedThreads, setOpenedThreads] = useState<ReadonlySet<number>>(
    () => new Set()
  );
  const threadOpened = useCallback(
    (rootId: number) => setOpenedThreads((ids) => new Set(ids).add(rootId)),
    []
  );
  const unread = useMutation({
    mutationFn: async (message: ChatMessage) => {
      await settled(); // Not overtaken by a read state still on its way.
      return markChatUnread(conversationId, message.id);
    },
    onMutate: (message) => {
      pause(message.id - 1);
      setReadMark(message.id - 1);
      setMarks((count) => count + 1);
      setOpenedThreads(new Set());
    },
    onSuccess: () => {
      invalidateUnreadCounts(queryClient);
    },
  });
  const hasUnreadReplies = useCallback(
    (message: ChatMessage) =>
      readMark != null &&
      (message.newestReplyOfOthersId ?? 0) > readMark &&
      !openedThreads.has(message.id),
    [readMark, openedThreads]
  );
  return {
    readMark: readMark ?? null,
    marks,
    markUnread: unread.mutate,
    threadOpened,
    hasUnreadReplies,
  };
}
