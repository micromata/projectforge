"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { Badge } from "@/components/ui/badge";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { ChatHostEntry } from "@/lib/rs/chat-types";
import { ChatDataTransferPill } from "./chat-host-pill";

/** The conversations of the data transfer areas as a list (marked by a pill leading to the area): title, last message, unread count. */
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
        <li key={entry.conversationId} className="flex flex-col px-3 py-2">
          <span className="flex items-center gap-2">
            <Link
              href={`/chat/${entry.conversationId}`}
              className="min-w-0 truncate font-semibold hover:underline"
            >
              <HighlightedText text={entry.title} query={highlight} />
            </Link>
            {entry.unread > 0 && (
              <Badge variant="destructive">
                {t("chat.room.unread", { arg0: entry.unread })}
              </Badge>
            )}
            {/* Beside the link, not in it: a link inside a link isn't valid. */}
            <ChatDataTransferPill link={entry.link} />
          </span>
          <span className="block text-xs text-muted-foreground">
            {entry.lastMessageAt &&
              formatTimestampMinutes(entry.lastMessageAt, formatContext)}
            {!entry.canWrite && ` · ${t("chat.conversation.readOnly")}`}
          </span>
        </li>
      ))}
    </ul>
  );
}
