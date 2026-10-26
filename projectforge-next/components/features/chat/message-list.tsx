"use client";

import { Fragment, useEffect, useLayoutEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { useChatContext } from "./chat-context";
import { MessageItem } from "./message-item";
import { useChatMessages } from "./use-chat-messages";

/** Below this distance from the end, the list counts as "at the end": new messages scroll it along. */
const AT_END_PX = 80;

/**
 * The messages of the conversation, oldest at the top, starting at the end (or at the message of a deep link).
 * Scrolling up loads older ones, keeping the position; new messages scroll along while at the end. A line marks
 * the first message unread when the conversation was opened (or as marked unread since). [onAtEnd] reports the
 * newest message shown at the end.
 */
export function MessageList({
  onAtEnd,
}: {
  onAtEnd: (newestId: number | null) => void;
}) {
  const t = useTranslations();
  const { conversation, highlightId, readMark } = useChatContext();
  const { query, messages } = useChatMessages(conversation.id);
  const scrollRef = useRef<HTMLDivElement>(null);
  const atEnd = useRef(true);
  const heightBefore = useRef<number | null>(null);
  const positioned = useRef(false);
  const newest = messages[messages.length - 1];
  const firstUnread = findFirstUnread(messages, readMark, conversation.me.id);

  // Keeps the position when older messages come in on top, follows new ones at the end.
  useLayoutEffect(() => {
    const element = scrollRef.current;
    if (!element || messages.length === 0) return;
    if (heightBefore.current != null) {
      element.scrollTop += element.scrollHeight - heightBefore.current;
      heightBefore.current = null;
    } else if (!positioned.current) {
      positioned.current = true;
      const target =
        highlightId != null
          ? document.getElementById(`chat-message-${highlightId}`)
          : null;
      if (target) target.scrollIntoView({ block: "center" });
      else element.scrollTop = element.scrollHeight;
    } else if (atEnd.current) {
      element.scrollTop = element.scrollHeight;
    }
  }, [messages, highlightId]);

  useEffect(() => {
    if (atEnd.current) onAtEnd(newest?.id ?? null);
  }, [newest?.id, onAtEnd]);

  const loadOlder = () => {
    if (!query.hasNextPage || query.isFetchingNextPage) return;
    heightBefore.current = scrollRef.current?.scrollHeight ?? null;
    void query.fetchNextPage();
  };
  const onScroll = () => {
    const element = scrollRef.current;
    if (!element) return;
    atEnd.current =
      element.scrollHeight - element.scrollTop - element.clientHeight <
      AT_END_PX;
    if (atEnd.current) onAtEnd(newest?.id ?? null);
    if (element.scrollTop < AT_END_PX) loadOlder();
  };

  return (
    <div
      ref={scrollRef}
      onScroll={onScroll}
      className="min-h-0 flex-1 overflow-y-auto py-2"
      data-chat-messages
      role="log"
      aria-live="polite"
    >
      {query.hasNextPage && (
        <div className="flex justify-center py-1">
          <Button
            variant="ghost"
            size="sm"
            disabled={query.isFetchingNextPage}
            onClick={loadOlder}
          >
            {t("chat.conversation.loadOlder")}
          </Button>
        </div>
      )}
      {query.isPending && (
        <div className="flex justify-center py-6">
          <Spinner className="h-5 w-5 border-2" />
        </div>
      )}
      {query.isSuccess && messages.length === 0 && (
        <p className="px-3 py-6 text-center text-sm text-muted-foreground">
          {t("chat.conversation.empty")}
        </p>
      )}
      {messages.map((message) => (
        <Fragment key={message.id}>
          {message.id === firstUnread && <NewMessagesLine />}
          <MessageItem message={message} />
        </Fragment>
      ))}
    </div>
  );
}

/** The first message of others newer than the read state [readMark], undefined if none (or never read). */
export function findFirstUnread(
  messages: ChatMessage[],
  readMark: number | null,
  meId: number
): number | undefined {
  if (readMark == null) return undefined;
  return messages.find((it) => it.id > readMark && it.author.id !== meId)?.id;
}

/** The line above the first unread message. */
export function NewMessagesLine() {
  const t = useTranslations();
  return (
    <div className="my-1 flex items-center gap-2 px-3 text-xs font-semibold text-destructive">
      <span className="h-px flex-1 bg-destructive/50" />
      {t("chat.conversation.newMessages")}
      <span className="h-px flex-1 bg-destructive/50" />
    </div>
  );
}
