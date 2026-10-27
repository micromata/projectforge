"use client";

import { useMemo } from "react";
import { sanitizeChatHtml } from "@/components/shared/rich-text";
import { cn } from "@/lib/utils";

const SIREN = "🚨";

/** Wraps the siren emoji in the text (not in attributes) in a span that flashes, see `.siren-blink` in globals.css. */
function flashSirens(html: string): string {
  if (!html.includes(SIREN) || typeof document === "undefined") return html;
  const template = document.createElement("template");
  template.innerHTML = html;
  const walker = document.createTreeWalker(
    template.content,
    NodeFilter.SHOW_TEXT
  );
  const texts: Text[] = [];
  for (let node = walker.nextNode(); node; node = walker.nextNode()) {
    if ((node as Text).data.includes(SIREN)) texts.push(node as Text);
  }
  for (const text of texts) {
    const fragment = document.createDocumentFragment();
    text.data.split(SIREN).forEach((part, index) => {
      if (index > 0) {
        const span = document.createElement("span");
        span.className = "siren-blink";
        span.textContent = SIREN;
        fragment.append(span);
      }
      fragment.append(part);
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
  const sanitized = useMemo(() => flashSirens(sanitizeChatHtml(html)), [html]);
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
