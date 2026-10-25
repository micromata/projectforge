"use client";

import { memo, useState } from "react";
import { useTranslations } from "next-intl";
import { UserAvatar } from "@/components/shared/user-avatar";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { useChatContext } from "./chat-context";
import { ChatHtml } from "./chat-html";
import { MessageActions } from "./message-actions";
import { MessageEditForm } from "./message-edit-form";
import { MessageImages } from "./message-images";
import { ReactionBar } from "./reaction-bar";
import { ThreadSummary } from "./thread-summary";
import { useMessageMutations } from "./use-message-mutations";

/**
 * One message: author, time and "edited" (MSG-02), the content or the placeholder of a deleted or hidden one,
 * images, reactions and, in the conversation, the summary of its thread.
 */
export const MessageItem = memo(function MessageItem({
  message,
  inThread = false,
}: {
  message: ChatMessage;
  inThread?: boolean;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  const { conversation, openThread, highlightId } = useChatContext();
  const { react } = useMessageMutations();
  const [editing, setEditing] = useState(false);
  const placeholder = message.deleted
    ? t("chat.message.deleted")
    : message.hidden && !message.content
      ? t("chat.message.hidden")
      : null;

  return (
    <article
      id={`chat-message-${message.id}`}
      aria-label={`${message.author.displayName}, ${formatTimestampMinutes(message.created, formatContext)}`}
      className={cn(
        "group relative flex gap-2.5 px-3 py-1.5 hover:bg-muted/40",
        message.mentionsMe && "border-l-2 border-primary bg-primary/5",
        highlightId === message.id && "bg-primary/10"
      )}
    >
      <UserAvatar name={message.author.displayName} className="mt-0.5" />
      <div className="min-w-0 flex-1">
        <header className="flex flex-wrap items-baseline gap-x-2">
          <span className="text-sm font-semibold">
            {message.author.displayName}
          </span>
          <time
            className="text-xs text-muted-foreground"
            dateTime={message.created ?? undefined}
          >
            {formatTimestampMinutes(message.created, formatContext)}
          </time>
          {message.editedAt && !message.deleted && (
            <span className="text-xs text-muted-foreground">
              ({t("chat.message.edited")}{" "}
              {formatTimestampMinutes(message.editedAt, formatContext)})
            </span>
          )}
        </header>
        {placeholder && (
          <p className="text-sm text-muted-foreground italic">{placeholder}</p>
        )}
        {message.hidden && message.content && (
          <p className="text-xs text-destructive">
            {t("chat.message.hiddenReason", {
              arg0: message.hiddenBy ?? "",
              arg1: message.hiddenReason ?? "",
            })}
          </p>
        )}
        {editing ? (
          <MessageEditForm message={message} onDone={() => setEditing(false)} />
        ) : (
          message.content && (
            <ChatHtml
              html={message.content}
              className={cn("text-sm", message.hidden && "opacity-60")}
            />
          )
        )}
        <MessageImages
          conversationId={conversation.id}
          imageIds={message.imageIds}
        />
        <ReactionBar
          reactions={message.reactions}
          disabled={!conversation.canWrite}
          onToggle={(emoji) => react.mutate({ id: message.id, emoji })}
        />
        {!inThread && (
          <ThreadSummary
            message={message}
            onOpen={() => openThread(message.id)}
          />
        )}
      </div>
      {!editing && (
        // Stays visible while its emoji picker or menu is open (outside, in a portal): they are anchored to it.
        <div className="absolute -top-3 right-3 hidden group-focus-within:flex group-hover:flex has-data-[state=open]:flex pointer-coarse:flex">
          <MessageActions
            message={message}
            inThread={inThread}
            onEdit={() => setEditing(true)}
          />
        </div>
      )}
    </article>
  );
});
