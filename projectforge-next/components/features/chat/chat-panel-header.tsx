"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Notification01Icon,
  NotificationOff01Icon,
} from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { UnreadBadge } from "@/components/shared/unread-badge";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { useChatContext } from "./chat-context";
import { ChatSearch } from "./chat-search";

/**
 * The head of a conversation: search, muting (NOT-07), a status pill while live updates fall back to polling and the
 * unread messages not seen yet, whose badge jumps to the next one.
 */
export function ChatPanelHeader({
  onJump,
  onMute,
  polling,
  unreadCount,
  onNextUnread,
  inline = false,
}: {
  onJump: (message: ChatMessage) => void;
  onMute: () => void;
  polling: boolean;
  unreadCount: number;
  onNextUnread: () => void;
  /** Shown within another row (the page's title row): no row of its own with a border. */
  inline?: boolean;
}) {
  const t = useTranslations();
  const { conversation } = useChatContext();
  const muteLabel = conversation.muted
    ? t("chat.conversation.unmute")
    : t("chat.conversation.mute");
  const nextUnreadLabel = t("chat.conversation.nextUnread", {
    arg0: unreadCount,
  });
  return (
    <div
      className={cn(
        "flex shrink-0 items-center gap-1",
        !inline && "border-b px-2 py-1"
      )}
    >
      <span
        className={cn(
          "min-w-0 truncate text-xs text-muted-foreground",
          !inline && "flex-1"
        )}
      >
        {!polling && conversation.muted ? t("chat.conversation.muted") : ""}
      </span>
      {polling && (
        <HintTooltip text={t("chat.conversation.polling")}>
          <Badge variant="outline" className="shrink-0">
            {t("chat.conversation.pollingLabel")}
          </Badge>
        </HintTooltip>
      )}
      <ChatSearch onJump={onJump} />
      <HintTooltip
        text={conversation.muted ? t("chat.conversation.muted") : null}
        plain
      >
        <Button
          variant="ghost"
          size="icon"
          aria-label={muteLabel}
          aria-pressed={conversation.muted}
          onClick={onMute}
        >
          <HugeiconsIcon
            icon={
              conversation.muted ? NotificationOff01Icon : Notification01Icon
            }
          />
        </Button>
      </HintTooltip>
      {unreadCount > 0 && (
        <HintTooltip text={nextUnreadLabel} plain>
          <Button
            variant="ghost"
            size="icon"
            aria-label={nextUnreadLabel}
            onClick={onNextUnread}
          >
            <UnreadBadge count={unreadCount} />
          </Button>
        </HintTooltip>
      )}
    </div>
  );
}
