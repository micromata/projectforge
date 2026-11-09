"use client";

import { memo, useState, type KeyboardEvent, type MouseEvent } from "react";
import { useFormatContext } from "@/hooks/use-format";
import { formatTime, formatTimestampMinutes } from "@/lib/format";
import { chatLinkPreviewImageUrl } from "@/lib/rs/chat";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { useChatContext } from "./chat-context";
import { LinkPreviewCard } from "./link-preview-card";
import { MessageActions } from "./message-actions";
import { MessageAttachments } from "./message-attachments";
import { MessageAuthor } from "./message-author";
import { ReadReceipt } from "./read-receipt";
import { MessageBubble } from "./message-bubble";
import { MessageHeader } from "./message-header";
import { ReactionBar } from "./reaction-bar";
import { ThreadSummary } from "./thread-summary";
import { useMessageMutations } from "./use-message-mutations";

/**
 * One message, grouped as in Google Chat: the first of a group shows the avatar and the header (author, a click
 * offers a direct message; time, "edited", MSG-02), a [continued] one only its time on hover. Then the bubble (quote,
 * content or placeholder), the preview of its link, files, reactions and, in the conversation, the summary of its
 * thread. The user's own messages are aligned right, without avatar and name.
 */
export const MessageItem = memo(function MessageItem({
  message,
  inThread = false,
  continued = false,
  readByPartner = false,
}: {
  message: ChatMessage;
  inThread?: boolean;
  /** Follows a message of the same author shortly before (see layoutMessages): no avatar, no header. */
  continued?: boolean;
  /** The newest own message the other one of a direct message has read: marked as read. */
  readByPartner?: boolean;
}) {
  const formatContext = useFormatContext();
  const {
    conversation,
    openThread,
    highlightId,
    observeSeen,
    actionsMessageId,
    setActionsMessageId,
  } = useChatContext();
  const { react, dismissLinkPreview } = useMessageMutations();
  const [editing, setEditing] = useState(false);
  // A click or tap on the bubble pins its actions (one message at a time), another one hides them. Beside that, a
  // mouse shows them on hovering in a wide column; touch devices and narrow columns have the click only.
  const actionsShown = actionsMessageId === message.id;
  const toggleActions = (event: MouseEvent | KeyboardEvent) => {
    // While editing, a click places the cursor in the text field and leaves the actions as they are.
    if (editing) return;
    if (
      (event.target as HTMLElement).closest(
        "a, button, img, summary, input, textarea, select, [contenteditable]"
      )
    )
      return;
    // Selecting text with the mouse ends in a click as well.
    if (window.getSelection()?.toString()) return;
    setActionsMessageId(actionsShown ? null : message.id);
  };

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
          "@container/message flex min-w-0 flex-1 flex-col gap-0.5",
          message.own ? "items-end" : "items-start"
        )}
      >
        {!continued && <MessageHeader message={message} />}
        {/* A click on the bubble shows its actions. They sit beside the bubble (left of an own one), top aligned; the
            bubble leaves them room. In a narrow column (phone, thread panel) there is none, so they never overlap the
            text but stand in a row below the bubble. */}
        <div
          className={cn(
            "relative min-w-0",
            editing
              ? "w-full"
              : "max-w-[85%] @min-[32rem]/message:max-w-[min(85%,calc(100%-7.5rem))]"
          )}
        >
          <div
            role="button"
            tabIndex={0}
            aria-expanded={actionsShown}
            onClick={toggleActions}
            onKeyDown={(event) => {
              if (event.target !== event.currentTarget) return;
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                toggleActions(event);
              }
            }}
          >
            <MessageBubble
              message={message}
              highlighted={highlightId === message.id}
              editing={editing}
              onEditDone={() => setEditing(false)}
            />
          </div>
          {!editing && (
            // Stays visible while its emoji picker or menu is open (outside, in a portal): they are anchored to it.
            <div
              className={cn(
                "z-10 mt-1 @min-[32rem]/message:absolute @min-[32rem]/message:top-0 @min-[32rem]/message:mt-0",
                actionsShown
                  ? "flex"
                  : "hidden has-data-[state=open]:flex @min-[32rem]/message:group-hover:flex",
                message.own
                  ? "justify-end @min-[32rem]/message:right-full @min-[32rem]/message:mr-1"
                  : "justify-start @min-[32rem]/message:left-full @min-[32rem]/message:ml-1"
              )}
            >
              <MessageActions
                message={message}
                inThread={inThread}
                onEdit={() => setEditing(true)}
              />
            </div>
          )}
        </div>
        {message.linkPreview && !editing && !message.fromBlocked && (
          <LinkPreviewCard
            preview={message.linkPreview}
            imageSrc={
              message.linkPreview.imageId
                ? chatLinkPreviewImageUrl(
                    message.id,
                    message.linkPreview.imageId
                  )
                : null
            }
            onRemove={
              message.own && conversation.canWrite
                ? () => dismissLinkPreview.mutate(message.id)
                : null
            }
          />
        )}
        <MessageAttachments
          conversationId={conversation.id}
          files={message.files}
        />
        <ReactionBar
          reactions={message.reactions}
          disabled={!conversation.canWrite}
          onToggle={(emoji) => react.mutate({ id: message.id, emoji })}
        />
        {readByPartner && <ReadReceipt />}
        {!inThread && (
          <ThreadSummary
            message={message}
            onOpen={() => openThread(message.id)}
          />
        )}
      </div>
    </article>
  );
});
