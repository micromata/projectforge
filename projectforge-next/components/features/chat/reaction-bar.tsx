"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import { useCoarsePointer } from "@/hooks/use-coarse-pointer";
import type { ChatReaction } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import { EmojiGlyph, emojiLabel } from "@/components/shared/emoji/custom-emoji";

const LONG_PRESS_MS = 500;

/**
 * The reactions of a message: per emoji the count, who reacted on hover (on a touch device: on a long press); a
 * click adds the own reaction or takes it back (MSG-10).
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
  if (reactions.length === 0) return null;
  return (
    <ul className="mt-1 flex flex-wrap gap-1">
      {reactions.map((reaction) => (
        <li key={reaction.emoji}>
          <ReactionChip
            reaction={reaction}
            onToggle={onToggle}
            disabled={disabled}
          />
        </li>
      ))}
    </ul>
  );
}

function ReactionChip({
  reaction,
  onToggle,
  disabled,
}: {
  reaction: ChatReaction;
  onToggle: (emoji: string) => void;
  disabled?: boolean;
}) {
  const t = useTranslations();
  const coarsePointer = useCoarsePointer();
  const [open, setOpen] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  // A long press ends in a click on release; that one must not toggle the reaction.
  const longPressed = useRef(false);

  const cancelPress = () => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
  };

  const button = (
    <button
      type="button"
      disabled={disabled}
      aria-pressed={reaction.mine}
      aria-label={`${t("chat.message.react")}: ${reaction.emoji} (${reaction.users.join(", ")})`}
      className={cn(
        "flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs hover:bg-muted",
        coarsePointer && "select-none [-webkit-touch-callout:none]",
        reaction.mine && "border-primary bg-primary/10"
      )}
      onClick={() => {
        if (longPressed.current) {
          longPressed.current = false;
          return;
        }
        onToggle(reaction.emoji);
      }}
      {...(coarsePointer && {
        onPointerDown: () => {
          longPressed.current = false;
          cancelPress();
          timer.current = setTimeout(() => {
            longPressed.current = true;
            setOpen(true);
          }, LONG_PRESS_MS);
        },
        onPointerUp: cancelPress,
        onPointerLeave: cancelPress,
        onPointerCancel: cancelPress,
        onContextMenu: (event) => event.preventDefault(),
      })}
    >
      <EmojiGlyph
        emoji={reaction.emoji}
        label={emojiLabel(t, reaction.emoji)}
      />
      <span className="tabular-nums">{reaction.count}</span>
    </button>
  );

  if (!coarsePointer) {
    return (
      <HintTooltip text={reaction.users.join(", ")} plain>
        {button}
      </HintTooltip>
    );
  }
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverAnchor asChild>{button}</PopoverAnchor>
      <PopoverContent className="w-auto gap-1">
        <ul>
          {reaction.users.map((user, index) => (
            <li key={index}>{user}</li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
