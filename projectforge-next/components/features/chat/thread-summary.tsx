"use client";

import { useTranslations } from "next-intl";
import { UserAvatar } from "@/components/shared/user-avatar";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { ChatMessage } from "@/lib/rs/chat-types";

/** Below a message with a thread: who replied, how often and when last (THR-03); a click opens it. */
export function ThreadSummary({
  message,
  onOpen,
}: {
  message: ChatMessage;
  onOpen: () => void;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  if (message.replyCount === 0) return null;
  const count =
    message.replyCount === 1
      ? t("chat.thread.reply")
      : t("chat.thread.replies", { arg0: message.replyCount });
  return (
    <button
      type="button"
      onClick={onOpen}
      className="mt-1 flex items-center gap-2 rounded-md px-1 py-0.5 text-xs hover:bg-muted"
    >
      <span className="flex -space-x-1.5">
        {message.replyAuthors.map((author) => (
          <UserAvatar
            key={author.id}
            name={author.displayName}
            className="size-5 text-[9px]"
          />
        ))}
      </span>
      <span className="font-semibold text-primary">{count}</span>
      {message.lastReplyAt && (
        <span className="text-muted-foreground">
          {t("chat.thread.lastReply", {
            arg0: formatTimestampMinutes(message.lastReplyAt, formatContext),
          })}
        </span>
      )}
    </button>
  );
}
