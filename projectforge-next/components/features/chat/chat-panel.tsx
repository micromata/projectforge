"use client";

import { useCallback, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import {
  chatConversationQueryKey,
  fetchChatConversation,
  muteChatConversation,
} from "@/lib/rs/chat";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { applyChatChanges } from "./chat-cache";
import { ChatComposer } from "./chat-composer";
import { ChatContextProvider, type ChatContextValue } from "./chat-context";
import { ChatPanelHeader } from "./chat-panel-header";
import { MessageList } from "./message-list";
import { ThreadPanel } from "./thread-panel";
import { useChatLive } from "./use-chat-live";
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
  // The read state when opened, for the "new" line: marking read meanwhile doesn't move it.
  const [lastReadAtOpen, setLastReadAtOpen] = useState<
    number | null | undefined
  >(undefined);
  if (conversation && lastReadAtOpen === undefined)
    setLastReadAtOpen(conversation.lastReadMessageId ?? null);
  const [threadRootId, setThreadRootId] = useState<number | null>(null);
  const [newestId, setNewestId] = useState(0);
  const markUpTo = useMarkRead(conversationId);
  const trackNewest = useCallback((messages: ChatMessage[]) => {
    setNewestId((id) => Math.max(id, ...messages.map((it) => it.id)));
  }, []);
  const live = useChatLive(
    conversationId,
    conversation?.changeSeq,
    trackNewest
  );
  const onAtEnd = useCallback(
    (id: number | null) =>
      markUpTo(Math.max(id ?? 0, newestId, conversation?.newestMessageId ?? 0)),
    [markUpTo, newestId, conversation?.newestMessageId]
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
            openThread: setThreadRootId,
            highlightId,
          }
        : null,
    [conversation, live.sync, highlightId]
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
    if (message.threadRootId) setThreadRootId(message.threadRootId);
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
        <MessageList
          lastReadMessageId={lastReadAtOpen ?? null}
          onAtEnd={onAtEnd}
        />
        {conversation.canWrite ? (
          <ChatComposer
            conversation={conversation}
            threadRootId={null}
            onSent={(message) => {
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
