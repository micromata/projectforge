"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { Badge } from "@/components/ui/badge";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { ChatHostEntry } from "@/lib/rs/chat-types";

/** The conversations of a host (e.g. the data transfer areas) as a list: title, last message, unread count. */
export function ChatHostList({
  entries,
  highlight,
}: {
  entries: ChatHostEntry[];
  /** The search term the list was narrowed by, marked in the titles. */
  highlight?: string;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  return (
    <ul className="flex max-w-3xl flex-col divide-y rounded-md border">
      {entries.map((entry) => (
        <li key={entry.conversationId}>
          <Link
            href={`/chat/${entry.conversationId}`}
            className="flex flex-col px-3 py-2 hover:underline"
          >
            <span className="flex items-center gap-2">
              <span className="truncate font-semibold">
                <HighlightedText text={entry.title} query={highlight} />
              </span>
              {entry.unread > 0 && (
                <Badge variant="destructive">
                  {t("chat.room.unread", { arg0: entry.unread })}
                </Badge>
              )}
            </span>
            <span className="block text-xs text-muted-foreground">
              {entry.lastMessageAt &&
                formatTimestampMinutes(entry.lastMessageAt, formatContext)}
              {!entry.canWrite && ` · ${t("chat.conversation.readOnly")}`}
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
