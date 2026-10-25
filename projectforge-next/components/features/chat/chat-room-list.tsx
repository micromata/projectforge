"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Settings02Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { ChatRoomEntry } from "@/lib/rs/chat-types";

/** The rooms as a list: name, description, participants, last message, unread count. */
export function ChatRoomList({
  rooms,
  onEdit,
}: {
  rooms: ChatRoomEntry[];
  onEdit: (roomId: number) => void;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  return (
    <ul className="flex max-w-3xl flex-col divide-y rounded-md border">
      {rooms.map((room) => (
        <li key={room.id} className="flex items-center gap-2 px-3 py-2">
          <Link
            href={
              room.conversationId ? `/chat/${room.conversationId}` : "/chat"
            }
            className="min-w-0 flex-1 hover:underline"
          >
            <span className="flex items-center gap-2">
              <span className="truncate font-semibold">{room.name}</span>
              {room.unread > 0 && (
                <Badge variant="destructive">
                  {t("chat.room.unread", { arg0: room.unread })}
                </Badge>
              )}
            </span>
            {room.description && (
              <span className="block truncate text-sm text-muted-foreground">
                {room.description}
              </span>
            )}
            <span className="block text-xs text-muted-foreground">
              {t("chat.room.participants", { arg0: room.participantCount })}
              {room.lastMessageAt &&
                ` · ${formatTimestampMinutes(room.lastMessageAt, formatContext)}`}
            </span>
          </Link>
          <Button
            variant="ghost"
            size="icon"
            aria-label={`${room.canEdit ? t("chat.room.edit") : t("chat.room.participants", { arg0: room.participantCount })}: ${room.name}`}
            onClick={() => onEdit(room.id)}
          >
            <HugeiconsIcon icon={Settings02Icon} />
          </Button>
        </li>
      ))}
    </ul>
  );
}
