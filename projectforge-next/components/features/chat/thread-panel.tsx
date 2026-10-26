"use client";

import { Fragment, useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Spinner } from "@/components/shared/spinner";
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
import { MessageItem } from "./message-item";
import { findFirstUnread, NewMessagesLine } from "./message-list";
import { useMessageMutations } from "./use-message-mutations";

/**
 * The thread of a message in a side panel (full screen on small devices, THR-04): the message, its replies,
 * following it and replying, a line above the first unread reply. Kept up to date by the live updates of the
 * conversation.
 */
export function ThreadPanel({
  rootId,
  onClose,
}: {
  rootId: number | null;
  onClose: () => void;
}) {
  const t = useTranslations();
  const { conversation, sync, readMark } = useChatContext();
  const queryClient = useQueryClient();
  const { follow } = useMessageMutations();
  const endRef = useRef<HTMLDivElement>(null);
  const query = useQuery({
    queryKey: chatThreadQueryKey(conversation.id, rootId ?? 0),
    queryFn: ({ signal }) => fetchChatThread(conversation.id, rootId!, signal),
    enabled: rootId != null,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  });
  const replies = query.data?.replies.length ?? 0;
  useEffect(() => {
    endRef.current?.scrollIntoView({ block: "end" });
  }, [replies]);
  const root = query.data?.root;
  const firstUnread = findFirstUnread(
    query.data?.replies ?? [],
    readMark,
    conversation.me.id
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
        <div className="min-h-0 flex-1 overflow-y-auto py-2">
          {query.isPending && rootId != null && (
            <div className="flex justify-center py-6">
              <Spinner className="h-5 w-5 border-2" />
            </div>
          )}
          {root && <MessageItem message={root} inThread />}
          {query.data && <div className="mx-3 my-1 border-t" />}
          {query.data?.replies.map((reply) => (
            <Fragment key={reply.id}>
              {reply.id === firstUnread && <NewMessagesLine />}
              <MessageItem message={reply} inThread />
            </Fragment>
          ))}
          <div ref={endRef} />
        </div>
        {rootId != null && conversation.canWrite && !root?.deleted && (
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
