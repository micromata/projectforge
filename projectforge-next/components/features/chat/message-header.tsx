"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useFormatContext } from "@/hooks/use-format";
import { formatTime, formatTimestampMinutes } from "@/lib/format";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { MessageAuthor } from "./message-author";

/**
 * The header of the first message of a group: the author (not for the user's own ones), the time (the day is
 * shown by the separator, the full timestamp on hover) and "edited" with the time of the edit on hover (MSG-02).
 */
export function MessageHeader({ message }: { message: ChatMessage }) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  return (
    <header
      className={cn(
        "flex flex-wrap items-baseline gap-x-2 px-1",
        message.own && "justify-end"
      )}
    >
      {!message.own && <MessageAuthor author={message.author} />}
      <HintTooltip
        text={formatTimestampMinutes(message.created, formatContext)}
        plain
        openOnTap
      >
        <time
          className="text-xs text-muted-foreground"
          dateTime={message.created ?? undefined}
        >
          {formatTime(message.created, formatContext)}
        </time>
      </HintTooltip>
      {message.editedAt && !message.deleted && (
        <HintTooltip
          text={formatTimestampMinutes(message.editedAt, formatContext)}
          plain
          openOnTap
        >
          <span className="text-xs text-muted-foreground">
            ({t("chat.message.edited")})
          </span>
        </HintTooltip>
      )}
    </header>
  );
}
