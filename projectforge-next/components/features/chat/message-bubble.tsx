"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { ChatHtml } from "./chat-html";
import { MessageEditForm } from "./message-edit-form";
import { QuotedMessage } from "./quoted-message";

/**
 * The bubble of a message: the message it quotes (MSG-08), the content or the placeholder of a deleted or hidden one,
 * the reason of hiding. The user's own ones are tinted, a mention of the user and the message of a deep link are
 * outlined. While editing, the form takes the width of the conversation instead.
 */
export function MessageBubble({
  message,
  highlighted,
  editing,
  onEditDone,
}: {
  message: ChatMessage;
  highlighted: boolean;
  editing: boolean;
  onEditDone: () => void;
}) {
  const t = useTranslations();
  const [shown, setShown] = useState(false);
  if (editing) {
    return (
      <div className="w-full">
        <MessageEditForm message={message} onDone={onEditDone} />
      </div>
    );
  }
  // The message of a blocked user stays folded until the reader asks for it.
  if (message.fromBlocked && !shown && !message.deleted) {
    return (
      <div className="flex w-fit items-center gap-2 rounded-2xl bg-muted px-3 py-1.5">
        <p className="text-sm text-muted-foreground italic">
          {t("chat.block.folded")}
        </p>
        <Button variant="ghost" size="sm" onClick={() => setShown(true)}>
          {t("chat.block.show")}
        </Button>
      </div>
    );
  }
  const placeholder = message.deleted
    ? t("chat.message.deleted")
    : message.hidden && !message.content
      ? t("chat.message.hidden")
      : null;
  return (
    <div
      className={cn(
        "w-fit max-w-full min-w-0 rounded-2xl px-3 py-1.5",
        message.own ? "bg-primary/10" : "bg-muted",
        message.mentionsMe && "ring-1 ring-primary",
        highlighted && "ring-2 ring-primary"
      )}
    >
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
      {message.quote && <QuotedMessage quote={message.quote} />}
      {message.content && (
        <ChatHtml
          html={message.content}
          className={cn("text-sm", message.hidden && "opacity-60")}
        />
      )}
    </div>
  );
}
