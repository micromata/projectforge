"use client";

import { useCallback, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import {
  chatConversationQueryKey,
  fetchChatConversation,
  markChatUnread,
  muteChatConversation,
} from "@/lib/rs/chat";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { applyChatChanges } from "./chat-cache";
import { ChatComposer } from "./chat-composer";
import { ChatContextProvider, type ChatContextValue } from "./chat-context";
import { ChatPanelHeader } from "./chat-panel-header";
import { MessageList } from "./message-list";
import { SecretOffers } from "./secret-offers";
import { ThreadPanel } from "./thread-panel";
import { useChatLive } from "./use-chat-live";
import { useSecretOffers } from "./use-chat-secret";
import { useMarkRead } from "./use-mark-read";

/**
 * A conversation (PF-CHAT): the messages, writing, threads in a side panel, live updates. Embeddable by any host
 * page (CTX-03); the conversation page shows it full size. [highlightId] is the message of a deep link (MSG-05).
 */
export function ChatPanel({
  conversationId,
  highlightId = null,
  className,
}: {
  conversationId: number;
  highlightId?: number | null;
  className?: string;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: chatConversationQueryKey(conversationId),
    queryFn: ({ signal }) => fetchChatConversation(conversationId, signal),
    refetchOnWindowFocus: false,
  });
  const conversation = query.data;
  // The read state when opened, for the "new" lines: marking read meanwhile doesn't move it, marking unread does.
  const [readMark, setReadMark] = useState<number | null | undefined>(
    undefined
  );
  if (conversation && readMark === undefined)
    setReadMark(conversation.lastReadMessageId ?? null);
  const [threadRootId, setThreadRootId] = useState<number | null>(null);
  // The threads opened since: their new replies are seen.
  const [openedThreads, setOpenedThreads] = useState<ReadonlySet<number>>(
    () => new Set()
  );
  const openThread = useCallback((rootId: number) => {
    setThreadRootId(rootId);
    setOpenedThreads((ids) => new Set(ids).add(rootId));
  }, []);
  const [newestId, setNewestId] = useState(0);
  const { markUpTo, pause, resume } = useMarkRead(conversationId);
  const trackNewest = useCallback((messages: ChatMessage[]) => {
    setNewestId((id) => Math.max(id, ...messages.map((it) => it.id)));
  }, []);
  const secrets = useSecretOffers(conversation);
  const live = useChatLive(
    conversationId,
    conversation?.changeSeq,
    trackNewest,
    secrets.refresh
  );
  const onAtEnd = useCallback(
    (id: number | null) =>
      markUpTo(Math.max(id ?? 0, newestId, conversation?.newestMessageId ?? 0)),
    [markUpTo, newestId, conversation?.newestMessageId]
  );
  const unread = useMutation({
    mutationFn: (message: ChatMessage) =>
      markChatUnread(conversationId, message.id),
    onMutate: (message) => {
      pause(message.id - 1);
      setReadMark(message.id - 1);
      setOpenedThreads(new Set());
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["chatRooms"] });
      void queryClient.invalidateQueries({ queryKey: ["chatDirect"] });
    },
  });
  const markUnread = unread.mutate;
  const hasUnreadReplies = useCallback(
    (message: ChatMessage) =>
      readMark != null &&
      (message.newestReplyOfOthersId ?? 0) > readMark &&
      !openedThreads.has(message.id),
    [readMark, openedThreads]
  );
  const mute = useMutation({
    mutationFn: (muted: boolean) => muteChatConversation(conversationId, muted),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: chatConversationQueryKey(conversationId),
      }),
  });
  const context = useMemo<ChatContextValue | null>(
    () =>
      conversation
        ? {
            conversation,
            sync: live.sync,
            openThread,
            highlightId,
            readMark: readMark ?? null,
            hasUnreadReplies,
            markUnread,
          }
        : null,
    [
      conversation,
      live.sync,
      openThread,
      highlightId,
      readMark,
      hasUnreadReplies,
      markUnread,
    ]
  );

  if (query.isError) {
    return (
      <FormAlert tone="error">{t("chat.conversation.notFound")}</FormAlert>
    );
  }
  if (!context || !conversation) {
    return (
      <div className="flex justify-center py-10">
        <Spinner className="h-6 w-6 border-2" />
      </div>
    );
  }
  const jump = (message: ChatMessage) => {
    if (message.threadRootId) openThread(message.threadRootId);
    else
      document
        .getElementById(`chat-message-${message.id}`)
        ?.scrollIntoView({ block: "center" });
  };
  return (
    <ChatContextProvider value={context}>
      <section
        className={cn("flex min-h-0 flex-1 flex-col", className)}
        aria-label={conversation.title}
      >
        <ChatPanelHeader
          onJump={jump}
          onMute={() => mute.mutate(!conversation.muted)}
          polling={live.mode === "polling"}
        />
        <MessageList onAtEnd={onAtEnd} />
        <SecretOffers sessions={secrets.sessions} onChange={secrets.refresh} />
        {conversation.canWrite ? (
          <ChatComposer
            conversation={conversation}
            threadRootId={null}
            onSent={(message) => {
              resume(); // Writing, the conversation is read again.
              applyChatChanges(queryClient, conversationId, [message]);
              void live.sync();
            }}
          />
        ) : (
          <p className="shrink-0 border-t px-3 py-2 text-sm text-muted-foreground">
            {t("chat.conversation.readOnly")}
          </p>
        )}
        <ThreadPanel
          rootId={threadRootId}
          onClose={() => setThreadRootId(null)}
        />
      </section>
    </ChatContextProvider>
  );
}
