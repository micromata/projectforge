"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { SmileIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { recentEmojis } from "./recent-emojis";

/** The emoji offered for reactions: the usual ones of a chat, no full emoji catalog. */
const EMOJIS = [
  "👍",
  "👎",
  "❤️",
  "😂",
  "😊",
  "😮",
  "😢",
  "🙏",
  "🎉",
  "👏",
  "🔥",
  "✅",
  "❌",
  "👀",
  "🤔",
  "💡",
  "🚀",
  "⚠️",
  "🐞",
  "☕",
  "💪",
  "🙌",
  "😅",
  "🤝",
];

/** A button opening the emoji of a reaction, the recently used ones first (MSG-10). */
export function EmojiPicker({
  onPick,
  ariaLabel,
}: {
  onPick: (emoji: string) => void;
  ariaLabel: string;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const recent = open ? recentEmojis.get() : [];
  const pick = (emoji: string) => {
    recentEmojis.add(emoji);
    setOpen(false);
    onPick(emoji);
  };
  const button = (emoji: string) => (
    <button
      key={emoji}
      type="button"
      className="rounded-md p-1 text-lg leading-none hover:bg-muted"
      aria-label={`${t("chat.message.react")}: ${emoji}`}
      onClick={() => pick(emoji)}
    >
      {emoji}
    </button>
  );
  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={ariaLabel}
        >
          <HugeiconsIcon icon={SmileIcon} />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto gap-1 p-1.5" align="start">
        {recent.length > 0 && (
          <div className="flex flex-wrap gap-0.5 border-b pb-1">
            {recent.map(button)}
          </div>
        )}
        <div className="grid grid-cols-8 gap-0.5">{EMOJIS.map(button)}</div>
      </PopoverContent>
    </Popover>
  );
}
