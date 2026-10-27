"use client";

import { useLocale, useTranslations } from "next-intl";
import { EmojiPicker, type Emoji } from "frimousse";
import { resolveEmojiData } from "./emoji-data";

/**
 * The whole emoji catalog with search and categories (frimousse). Only loaded when "More" is clicked
 * (`next/dynamic` in emoji-picker.tsx), together with its data (emoji-data.ts), so the chat itself stays small.
 */
export default function EmojiFullPicker({
  onPick,
}: {
  onPick: (emoji: string) => void;
}) {
  const t = useTranslations();
  const locale = useLocale();
  return (
    <EmojiPicker.Root
      className="isolate flex h-80 w-72 flex-col"
      locale={locale.startsWith("de") ? "de" : "en"}
      columns={8}
      resolveEmojiData={resolveEmojiData}
      onEmojiSelect={(emoji: Emoji) => onPick(emoji.emoji)}
    >
      <EmojiPicker.Search
        className="z-10 mx-1 mb-1 rounded-md border bg-background px-2 py-1 text-sm"
        placeholder={t("chat.emoji.search")}
        aria-label={t("chat.emoji.search")}
      />
      <EmojiPicker.Viewport className="relative flex-1 outline-hidden">
        <EmojiPicker.Loading className="absolute inset-0 flex items-center justify-center text-sm text-muted-foreground">
          {t("chat.emoji.loading")}
        </EmojiPicker.Loading>
        <EmojiPicker.Empty className="absolute inset-0 flex items-center justify-center text-sm text-muted-foreground">
          {t("chat.emoji.empty")}
        </EmojiPicker.Empty>
        <EmojiPicker.List
          className="select-none pb-1"
          components={{
            CategoryHeader: ({ category, ...props }) => (
              <div
                className="bg-background px-2 py-1 text-xs font-medium text-muted-foreground"
                {...props}
              >
                {category.label}
              </div>
            ),
            Emoji: ({ emoji, ...props }) => (
              <button
                className="flex size-8 items-center justify-center rounded-md text-lg data-[active]:bg-muted"
                {...props}
              >
                {emoji.emoji}
              </button>
            ),
          }}
        />
      </EmojiPicker.Viewport>
    </EmojiPicker.Root>
  );
}
