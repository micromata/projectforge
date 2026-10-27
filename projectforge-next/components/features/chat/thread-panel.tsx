"use client";

import { Fragment, useEffect, useMemo, useRef } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { fetchChatThread } from "@/lib/rs/chat";
import { applyChatChanges, chatThreadQueryKey } from "./chat-cache";
import { useChatContext } from "./chat-context";
import { ChatComposer } from "./chat-composer";
import { DaySeparator } from "./day-separator";
import { layoutMessages } from "./message-grouping";
import { MessageItem } from "./message-item";
import { findFirstUnread, NewMessagesLine } from "./message-list";
import { isAtEnd, useFollowEnd } from "./use-follow-end";
import { useMarkThreadRead } from "./use-mark-thread-read";
import { useMessageMutations } from "./use-message-mutations";

/**
 * The thread of a message in a side panel (full screen on small devices, THR-04): the message, its replies,
 * following it and replying, a line above the first unread reply; mentions in it are resolved. Opened at the reply
 * [focusId] (jumping to the next unread one), otherwise at its end. Kept up to date
 * by the live updates of the conversation.
 */
export function ThreadPanel({
  rootId,
  focusId = null,
  onClose,
}: {
  rootId: number | null;
  /** The reply to scroll to (the next unread one), otherwise the end. */
  focusId?: number | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  const { conversation, sync, readMark } = useChatContext();
  const queryClient = useQueryClient();
  const { follow } = useMessageMutations();
  const endRef = useRef<HTMLDivElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const atEnd = useRef(true);
  const contentRef = useFollowEnd(scrollRef, atEnd);
  const query = useQuery({
    queryKey: chatThreadQueryKey(conversation.id, rootId ?? 0),
    queryFn: ({ signal }) => fetchChatThread(conversation.id, rootId!, signal),
    enabled: rootId != null,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  const replies = query.data?.replies.length ?? 0;
  const focused = query.data?.replies.some((it) => it.id === focusId) === true;
  // The reply scrolled to once: new replies afterwards are followed at the end again.
  const focusedOnce = useRef<number | null>(null);
  useEffect(() => {
    if (rootId == null) focusedOnce.current = null; // Closed: jumped to again later.
  }, [rootId]);
  useEffect(() => {
    const target =
      focused && focusedOnce.current !== focusId
        ? document.getElementById(`chat-message-${focusId}`)
        : null;
    if (target) {
      focusedOnce.current = focusId;
      target.scrollIntoView({ block: "center" });
    } else endRef.current?.scrollIntoView({ block: "end" });
    if (scrollRef.current) atEnd.current = isAtEnd(scrollRef.current);
  }, [replies, focused, focusId]);
  useMarkThreadRead(conversation.id, rootId, query.data?.replies);
  const root = query.data?.root;
  const firstUnread = findFirstUnread(
    query.data?.replies ?? [],
    readMark,
    conversation.me.id
  );
  // Laid out with the root, so a reply on the day of the root gets no separator; the first reply starts a group of
  // its own all the same, below the line under the root.
  const replyLayout = useMemo(
    () =>
      layoutMessages(
        [...(root ? [root] : []), ...(query.data?.replies ?? [])],
        formatContext,
        firstUnread
      )
        .slice(root ? 1 : 0)
        .map((it, index) => (index === 0 ? { ...it, continued: false } : it)),
    [root, query.data?.replies, formatContext, firstUnread]
  );

  return (
    <Sheet open={rootId != null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent
        side="right"
        className="flex flex-col gap-0 p-0 data-[side=right]:w-full data-[side=right]:sm:max-w-lg"
      >
        <SheetHeader className="flex-row items-center gap-2 border-b pr-12">
          <SheetTitle className="flex-1">{t("chat.thread.title")}</SheetTitle>
          <SheetDescription className="sr-only">
            {conversation.title}
          </SheetDescription>
          {root && (
            <Button
              variant="outline"
              size="sm"
              onClick={() =>
                follow.mutate({ id: root.id, value: !root.following })
              }
            >
              {root.following
                ? t("chat.thread.unfollow")
                : t("chat.thread.follow")}
            </Button>
          )}
        </SheetHeader>
        <div
          ref={scrollRef}
          onScroll={(event) => (atEnd.current = isAtEnd(event.currentTarget))}
          className="min-h-0 flex-1 overflow-y-auto py-2"
          data-chat-messages
        >
          <div ref={contentRef}>
            {query.isPending && rootId != null && (
              <div className="flex justify-center py-6">
                <Spinner className="h-5 w-5 border-2" />
              </div>
            )}
            {root?.created && <DaySeparator timestamp={root.created} />}
            {root && <MessageItem message={root} inThread />}
            {query.data && <div className="mx-3 my-1 border-t" />}
            {replyLayout.map(({ message: reply, dayStart, continued }) => (
              <Fragment key={reply.id}>
                {dayStart && <DaySeparator timestamp={dayStart} />}
                {reply.id === firstUnread && <NewMessagesLine />}
                <MessageItem message={reply} inThread continued={continued} />
              </Fragment>
            ))}
            <div ref={endRef} />
          </div>
        </div>
        {rootId != null &&
          conversation.canWrite &&
          !root?.deleted &&
          !root?.hidden && (
            <ChatComposer
              conversation={conversation}
              threadRootId={rootId}
              autoFocus
              onSent={(message) => {
                applyChatChanges(queryClient, conversation.id, [message]);
                void sync();
              }}
            />
          )}
      </SheetContent>
    </Sheet>
  );
}
