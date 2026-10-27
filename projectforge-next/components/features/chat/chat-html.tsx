"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { sanitizeChatHtml } from "@/components/shared/rich-text";
import { cn } from "@/lib/utils";
import {
  CUSTOM_EMOJIS,
  CUSTOM_EMOJI_PATTERN,
  customEmojiClass,
  isCustomEmoji,
  type CustomEmojiCode,
} from "./custom-emoji";

const SIREN = "🚨";
const EMOJI_PATTERN = new RegExp(
  `(${SIREN}|${CUSTOM_EMOJI_PATTERN.source})`,
  "g"
);

/**
 * Decorates the emoji in the text (not in attributes, nor in code): the siren flashes (`.siren-blink`, globals.css),
 * the shortcodes of the custom emoji are drawn (`.chat-emoji`).
 */
function decorateEmoji(html: string, label: (code: CustomEmojiCode) => string) {
  if (typeof document === "undefined") return html;
  if (!html.includes(SIREN) && !html.includes(":siren-")) return html;
  const template = document.createElement("template");
  template.innerHTML = html;
  const walker = document.createTreeWalker(
    template.content,
    NodeFilter.SHOW_TEXT
  );
  const texts: Text[] = [];
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    const text = node as Text;
    if (text.parentElement?.closest("code, pre")) continue;
    if (new RegExp(EMOJI_PATTERN).test(text.data)) texts.push(text);
  }
  for (const text of texts) {
    const fragment = document.createDocumentFragment();
    text.data.split(EMOJI_PATTERN).forEach((part, index) => {
      if (index % 2 === 0) {
        if (part) fragment.append(part);
        return;
      }
      const span = document.createElement("span");
      if (isCustomEmoji(part)) {
        span.className = customEmojiClass(part);
        span.setAttribute("role", "img");
        span.setAttribute("aria-label", label(part));
      } else {
        span.className = "siren-blink";
        span.textContent = part;
      }
      fragment.append(span);
    });
    text.replaceWith(fragment);
  }
  return template.innerHTML;
}

/** The content of a chat message, sanitized ([sanitizeChatHtml]), with the mentions highlighted. */
export function ChatHtml({
  html,
  className,
}: {
  html: string;
  className?: string;
}) {
  const t = useTranslations();
  const sanitized = useMemo(
    () =>
      decorateEmoji(sanitizeChatHtml(html), (code) => t(CUSTOM_EMOJIS[code])),
    [html, t]
  );
  return (
    <div
      className={cn(
        "rich-text space-y-0.5 font-normal break-words text-foreground/85 [&_:is(h1,h2,h3,h4)]:text-sm [&_a]:underline [&_ol]:list-decimal [&_ol]:pl-6 [&_strong]:font-semibold [&_ul]:list-disc [&_ul]:pl-6",
        "[&_[data-type=mention]]:rounded-sm [&_[data-type=mention]]:font-medium [&_[data-type=mention]]:text-primary",
        className
      )}
      dangerouslySetInnerHTML={{ __html: sanitized }}
    />
  );
}
