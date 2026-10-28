import { cn } from "@/lib/utils";

/**
 * The emoji of the chat that no Unicode character exists for. They are stored like any emoji (reaction or text),
 * as their shortcode, and drawn with CSS (`.chat-emoji` in globals.css). The key is the shortcode, the value the
 * i18n key of its name.
 */
export const CUSTOM_EMOJIS = {
  ":siren-blue:": "chat.emoji.siren.blue",
  ":siren-red:": "chat.emoji.siren.red",
} as const;

export type CustomEmojiCode = keyof typeof CUSTOM_EMOJIS;

/** Matches the shortcodes in a text. */
export const CUSTOM_EMOJI_PATTERN = /:siren-(?:blue|red):/g;

export function isCustomEmoji(value: string): value is CustomEmojiCode {
  return Object.hasOwn(CUSTOM_EMOJIS, value);
}

/** The CSS class of a custom emoji, e.g. `chat-emoji-siren-blue` for `:siren-blue:`. */
export function customEmojiClass(code: CustomEmojiCode): string {
  return `chat-emoji chat-emoji-${code.slice(1, -1)}`;
}

/** The translated name of a custom emoji, undefined for any other one. */
export function emojiLabel(
  t: (key: string) => string,
  emoji: string
): string | undefined {
  return isCustomEmoji(emoji) ? t(CUSTOM_EMOJIS[emoji]) : undefined;
}

/** An emoji of a reaction or the picker: a custom one drawn, any other one as the character. */
export function EmojiGlyph({
  emoji,
  label,
  className,
}: {
  emoji: string;
  /** The name of a custom emoji, translated. */
  label?: string;
  className?: string;
}) {
  if (isCustomEmoji(emoji)) {
    return (
      <span
        role="img"
        aria-label={label ?? emoji}
        className={cn(customEmojiClass(emoji), className)}
      />
    );
  }
  return (
    <span className={cn(emoji === "🚨" && "siren-blink", className)}>
      {emoji}
    </span>
  );
}
