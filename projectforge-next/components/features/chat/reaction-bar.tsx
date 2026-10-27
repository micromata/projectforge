"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { ChatReaction } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { EmojiGlyph, emojiLabel } from "./custom-emoji";

/**
 * The reactions of a message: per emoji the count, who reacted on hover; a click adds the own reaction or takes
 * it back (MSG-10).
 */
export function ReactionBar({
  reactions,
  onToggle,
  disabled,
}: {
  reactions: ChatReaction[];
  onToggle: (emoji: string) => void;
  disabled?: boolean;
}) {
  const t = useTranslations();
  if (reactions.length === 0) return null;
  return (
    <ul className="mt-1 flex flex-wrap gap-1">
      {reactions.map((reaction) => (
        <li key={reaction.emoji}>
          <HintTooltip text={reaction.users.join(", ")} plain>
            <button
              type="button"
              disabled={disabled}
              aria-pressed={reaction.mine}
              aria-label={`${t("chat.message.react")}: ${reaction.emoji} (${reaction.users.join(", ")})`}
              className={cn(
                "flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs hover:bg-muted",
                reaction.mine && "border-primary bg-primary/10"
              )}
              onClick={() => onToggle(reaction.emoji)}
            >
              <EmojiGlyph
                emoji={reaction.emoji}
                label={emojiLabel(t, reaction.emoji)}
              />
              <span className="tabular-nums">{reaction.count}</span>
            </button>
          </HintTooltip>
        </li>
      ))}
    </ul>
  );
}
