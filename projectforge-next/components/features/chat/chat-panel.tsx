"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import {
  chatConversationQueryKey,
  chatPartnerReadQueryKey,
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
import { SecretOffers } from "./secret-offers";
import { ThreadPanel } from "./thread-panel";
import { useChatLive } from "./use-chat-live";
import { useSecretOffers } from "./use-chat-secret";
import { useMarkRead } from "./use-mark-read";
import { useQuoting } from "./use-quoting";
import { useReadMark } from "./use-read-mark";
import { useUnreadJump } from "./use-unread-jump";

/**
 * A conversation (PF-CHAT): the messages, writing, threads in a side panel, live updates. Embeddable by any host
 * page (CTX-03); the conversation page shows it full size. [highlightId] is the message of a deep link (MSG-05),
 * [threadRootId] the thread opened with it for a reply (e.g. a match of the search across conversations).
 */
export function ChatPanel({
  conversationId,
  highlightId = null,
  threadRootId: initialThreadRootId = null,
  headerSlot = null,
  className,
}: {
  conversationId: number;
  highlightId?: number | null;
  threadRootId?: number | null;
  /** An element of the host page (e.g. its title row) to show search, muting and the like in, instead of a row of its own. */
  headerSlot?: HTMLElement | null;
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
  const { markUpTo, pause, resume, settled } = useMarkRead(conversationId);
  const { readMark, marks, markUnread, threadOpened, hasUnreadReplies } =
    useReadMark(conversationId, conversation, pause, settled);
  const { quoting, setQuoting } = useQuoting();
  const [threadRootId, setThreadRootId] = useState<number | null>(
    initialThreadRootId
  );
  const [threadFocusId, setThreadFocusId] = useState<number | null>(
    initialThreadRootId ? highlightId : null
  );
  useEffect(() => {
    if (initialThreadRootId) threadOpened(initialThreadRootId);
    // The thread of the deep link on mount only, as the state above.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const openThread = useCallback(
    (rootId: number, focusId?: number) => {
      if (rootId !== threadRootId) setQuoting(true, null); // A quote of another thread.
      setThreadRootId(rootId);
      setThreadFocusId(focusId ?? null);
      threadOpened(rootId);
    },
    [threadRootId, setQuoting, threadOpened]
  );
  const jump = useCallback(
    (target: { id: number; threadRootId: number | null }) => {
      if (target.threadRootId) openThread(target.threadRootId, target.id);
      else
        document
          .getElementById(`chat-message-${target.id}`)
          ?.scrollIntoView({ block: "center" });
    },
    [openThread]
  );
  const [newestId, setNewestId] = useState(0);
  const closeThread = useCallback(() => setThreadRootId(null), []);
  const unreadJump = useUnreadJump(
    conversationId,
    readMark,
    marks,
    openThread,
    closeThread
  );
  const known = Math.max(newestId, conversation?.newestMessageId ?? 0);
  const meId = conversation?.me.id;
  const refreshUnread = unreadJump.refresh;
  const refreshPartnerRead = useCallback(
    () =>
      queryClient.invalidateQueries({
        queryKey: chatPartnerReadQueryKey(conversationId),
      }),
    [queryClient, conversationId]
  );
  const trackNewest = useCallback(
    (messages: ChatMessage[]) => {
      setNewestId((id) => Math.max(id, ...messages.map((it) => it.id)));
      // New ones of others: unread as well, until seen.
      if (messages.some((it) => it.id > known && it.author.id !== meId))
        refreshUnread();
      // A message of the other one counts as read by it, up to itself.
      if (messages.some((it) => it.id > known && it.author.id !== meId))
        void refreshPartnerRead();
    },
    [known, meId, refreshUnread, refreshPartnerRead]
  );
  const secrets = useSecretOffers(conversation);
  const live = useChatLive(
    conversationId,
    conversation?.changeSeq,
    trackNewest,
    secrets.refresh,
    refreshPartnerRead
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
            openThread,
            jump,
            quoting,
            setQuoting,
            highlightId: unreadJump.jumpedId ?? highlightId,
            readMark,
            hasUnreadReplies,
            markUnread,
            observeSeen: unreadJump.observe,
          }
        : null,
    [
      conversation,
      live.sync,
      openThread,
      jump,
      quoting,
      setQuoting,
      unreadJump.jumpedId,
      highlightId,
      readMark,
      hasUnreadReplies,
      markUnread,
      unreadJump.observe,
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
  return (
    <ChatContextProvider value={context}>
      <section
        className={cn("flex min-h-0 flex-1 flex-col", className)}
        aria-label={conversation.title}
      >
        {(() => {
          const header = (
            <ChatPanelHeader
              onJump={jump}
              unreadCount={unreadJump.count}
              onNextUnread={unreadJump.jump}
              onMute={() => mute.mutate(!conversation.muted)}
              polling={live.mode === "polling"}
              inline={headerSlot != null}
            />
          );
          return headerSlot ? createPortal(header, headerSlot) : header;
        })()}
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
          focusId={threadFocusId}
          onClose={closeThread}
        />
      </section>
    </ChatContextProvider>
  );
}
