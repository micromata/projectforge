"use client";

import { memo, useState } from "react";
import { useFormatContext } from "@/hooks/use-format";
import { formatTime, formatTimestampMinutes } from "@/lib/format";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { useChatContext } from "./chat-context";
import { MessageActions } from "./message-actions";
import { MessageAttachments } from "./message-attachments";
import { MessageAuthor } from "./message-author";
import { MessageBubble } from "./message-bubble";
import { MessageHeader } from "./message-header";
import { ReactionBar } from "./reaction-bar";
import { ThreadSummary } from "./thread-summary";
import { useMessageMutations } from "./use-message-mutations";

/**
 * One message, grouped as in Google Chat: the first of a group shows the avatar and the header (author, a click
 * offers a direct message; time, "edited", MSG-02), a [continued] one only its time on hover. Then the bubble (quote,
 * content or placeholder), files, reactions and, in the conversation, the summary of its thread. The user's own
 * messages are aligned right, without avatar and name.
 */
export const MessageItem = memo(function MessageItem({
  message,
  inThread = false,
  continued = false,
}: {
  message: ChatMessage;
  inThread?: boolean;
  /** Follows a message of the same author shortly before (see layoutMessages): no avatar, no header. */
  continued?: boolean;
}) {
  const formatContext = useFormatContext();
  const { conversation, openThread, highlightId, observeSeen } =
    useChatContext();
  const { react } = useMessageMutations();
  const [editing, setEditing] = useState(false);

  return (
    <article
      id={`chat-message-${message.id}`}
      ref={message.author.id !== conversation.me.id ? observeSeen : undefined}
      data-message-id={message.id}
      aria-label={`${message.author.displayName}, ${formatTimestampMinutes(message.created, formatContext)}`}
      className={cn(
        "group relative flex gap-2 px-3 pb-0.5 transition-colors",
        continued ? "pt-0.5" : "pt-2"
      )}
    >
      {!message.own && (
        <div className="w-7 shrink-0">
          {continued ? (
            <time
              className="invisible block pt-2 text-center text-[10px] text-muted-foreground group-hover:visible"
              dateTime={message.created ?? undefined}
            >
              {formatTime(message.created, formatContext)}
            </time>
          ) : (
            <MessageAuthor author={message.author} avatar />
          )}
        </div>
      )}
      {message.own && continued && (
        <time
          className="invisible mt-2 shrink-0 self-start text-[10px] text-muted-foreground group-hover:visible"
          dateTime={message.created ?? undefined}
        >
          {formatTime(message.created, formatContext)}
        </time>
      )}
      <div
        className={cn(
          "flex min-w-0 flex-1 flex-col gap-0.5",
          message.own ? "items-end" : "items-start"
        )}
      >
        {!continued && <MessageHeader message={message} />}
        <MessageBubble
          message={message}
          highlighted={highlightId === message.id}
          editing={editing}
          onEditDone={() => setEditing(false)}
        />
        <MessageAttachments
          conversationId={conversation.id}
          files={message.files}
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
        <div
          className={cn(
            "absolute hidden group-focus-within:flex group-hover:flex has-data-[state=open]:flex pointer-coarse:flex",
            // The first of a group has a header row: stay inside the row, not over the divider above it.
            continued ? "-top-3" : "top-0",
            message.own ? "left-3" : "right-3"
          )}
        >
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
