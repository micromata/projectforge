"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { FormAlert } from "@/components/shared/form-alert";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { RefName } from "@/components/shared/ref-list";
import { richTextToPlainText } from "@/components/shared/rich-text";
import { Spinner } from "@/components/shared/spinner";
import { Badge } from "@/components/ui/badge";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import {
  CHAT_HOST_CHAT_ROOM,
  CHAT_HOST_DATA_TRANSFER,
  CHAT_HOST_DIRECT,
  CHAT_SEARCH_ALL_MIN_LENGTH,
  searchAllChatMessages,
} from "@/lib/rs/chat";
import type { ChatSearchHit } from "@/lib/rs/chat-types";

/** The characters of context shown before and after the match. */
const CONTEXT_LENGTH = 60;

/** The text around the first match of the term, cut with an ellipsis; the start of the text without one. */
function excerpt(text: string, term: string): string {
  const index = text.toLowerCase().indexOf(term.toLowerCase());
  if (index < 0) return text.slice(0, 2 * CONTEXT_LENGTH);
  const start = Math.max(0, index - CONTEXT_LENGTH);
  const end = Math.min(text.length, index + term.length + CONTEXT_LENGTH);
  return `${start > 0 ? "…" : ""}${text.slice(start, end)}${end < text.length ? "…" : ""}`;
}

/** The deep link to the message, opening its thread for a reply. */
function messageHref(hit: ChatSearchHit): string {
  const { id, threadRootId } = hit.message;
  const thread = threadRootId ? `&thread=${threadRootId}` : "";
  return `/chat/${hit.conversationId}?message=${id}${thread}`;
}

/**
 * The conversations with messages matching the search of the chat page (SRCH-02), one entry each: title, kind,
 * number of matches and the newest match around the term; a click jumps to that message.
 */
export function ChatSearchHitList({
  hits,
  term,
}: {
  hits: ChatSearchHit[];
  term: string;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  const kind = (hostType: string) => {
    switch (hostType) {
      case CHAT_HOST_CHAT_ROOM:
        return t("chat.search.kind.chatRoom");
      case CHAT_HOST_DIRECT:
        return t("chat.search.kind.direct");
      case CHAT_HOST_DATA_TRANSFER:
        return t("chat.dataTransfer._");
      default:
        return null;
    }
  };
  return (
    <ul className="flex max-w-3xl flex-col divide-y rounded-md border">
      {hits.map((hit) => {
        const label = kind(hit.hostType);
        return (
          <li key={hit.conversationId}>
            <Link
              href={messageHref(hit)}
              className="group flex flex-col gap-0.5 px-3 py-1.5"
            >
              <span className="flex min-w-0 items-center gap-2">
                <span className="truncate text-sm font-medium group-hover:underline">
                  {hit.title}
                </span>
                {label && (
                  <Badge
                    variant="secondary"
                    className="shrink-0 px-1.5 py-0 text-[11px] font-normal"
                  >
                    {label}
                  </Badge>
                )}
              </span>
              <span className="line-clamp-2 text-sm text-muted-foreground">
                <HighlightedText
                  text={excerpt(
                    richTextToPlainText(hit.message.content ?? ""),
                    term
                  )}
                  query={term}
                />
              </span>
              <span className="text-xs text-muted-foreground">
                <RefName value={hit.message.author} />
                {hit.message.created &&
                  `, ${formatTimestampMinutes(hit.message.created, formatContext)}`}
                {` · ${t("chat.search.hits", { arg0: hit.hitCount })}`}
              </span>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

/**
 * The section "Found messages" of the chat page: the messages of all readable conversations containing the term
 * (SRCH-02), from [CHAT_SEARCH_ALL_MIN_LENGTH] characters on; nothing for a shorter term.
 */
export function ChatMessageSearch({ term: search }: { term: string }) {
  const t = useTranslations();
  const term = search.trim();
  const query = useQuery({
    queryKey: ["chatSearchAll", term],
    queryFn: ({ signal }) => searchAllChatMessages(term, signal),
    enabled: term.length >= CHAT_SEARCH_ALL_MIN_LENGTH,
  });
  if (term.length < CHAT_SEARCH_ALL_MIN_LENGTH) return null;
  return (
    <section
      className="flex flex-col gap-3 px-4 pb-6"
      aria-labelledby="chatSearchHeading"
    >
      <h2 id="chatSearchHeading" className="text-sm font-semibold">
        {t("chat.search.messages")}
      </h2>
      {query.isError && (
        <FormAlert tone="error">{t("access.exception.noAccess")}</FormAlert>
      )}
      {query.isPending && <Spinner className="h-5 w-5 border-2" />}
      {query.data?.length === 0 && (
        <p className="text-sm text-muted-foreground">
          {t("chat.search.noResults")}
        </p>
      )}
      {query.data && query.data.length > 0 && (
        <ChatSearchHitList hits={query.data} term={term} />
      )}
    </section>
  );
}
