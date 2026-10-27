"use client";

import type { MouseEvent } from "react";
import { useTranslations } from "next-intl";
import { RefName } from "@/components/shared/ref-list";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { ChatQuote } from "@/lib/rs/chat-types";
import { useChatContext } from "./chat-context";

const HIGHLIGHT_CLASS = "bg-primary/10";
const HIGHLIGHT_MS = 1500;

/**
 * The message quoted by a message (MSG-08): author, time and the beginning of its text, or the placeholder of a
 * deleted or hidden one. A click shows it: in the same list (conversation or thread) if it is there, highlighted for
 * a moment, else via [ChatContextValue.jump] (scrolled to in the conversation, or its thread opened).
 */
export function QuotedMessage({ quote }: { quote: ChatQuote }) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  const { jump } = useChatContext();
  const text =
    quote.excerpt ??
    (quote.deleted ? t("chat.message.deleted") : t("chat.message.hidden"));
  const show = (event: MouseEvent<HTMLButtonElement>) => {
    const target = event.currentTarget
      .closest("[data-chat-messages]")
      ?.querySelector<HTMLElement>(`#chat-message-${quote.id}`);
    if (!target) {
      jump(quote);
      return;
    }
    target.scrollIntoView({ block: "center", behavior: "smooth" });
    target.classList.add(HIGHLIGHT_CLASS);
    window.setTimeout(
      () => target.classList.remove(HIGHLIGHT_CLASS),
      HIGHLIGHT_MS
    );
  };
  return (
    <button
      type="button"
      className="my-1 block w-full max-w-xl rounded-sm border-l-2 border-primary bg-background/60 px-2 py-1 text-left text-xs hover:bg-background"
      aria-label={t("chat.message.quote.of", {
        arg0: quote.author.displayName,
      })}
      onClick={show}
    >
      <span className="block text-muted-foreground">
        <span className="font-medium text-foreground">
          <RefName value={quote.author} />
        </span>
        , {formatTimestampMinutes(quote.created, formatContext)}
      </span>
      <span
        className={
          quote.excerpt
            ? "line-clamp-2 break-words"
            : "text-muted-foreground italic"
        }
      >
        {text}
      </span>
    </button>
  );
}
