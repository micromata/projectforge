"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { richTextToPlainText } from "@/components/shared/rich-text";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import { searchChatMessages } from "@/lib/rs/chat";
import type { ChatMessage } from "@/lib/rs/chat-types";
import { useChatContext } from "./chat-context";

/** Searching the messages of the conversation and its threads by text (SRCH-01); a hit jumps to the message. */
export function ChatSearch({
  onJump,
}: {
  onJump: (message: ChatMessage) => void;
}) {
  const t = useTranslations();
  const formatContext = useFormatContext();
  const { conversation } = useChatContext();
  const [open, setOpen] = useState(false);
  const [term, setTerm] = useState("");
  const q = term.trim();
  const query = useQuery({
    queryKey: ["chatSearch", conversation.id, q],
    queryFn: ({ signal }) => searchChatMessages(conversation.id, q, signal),
    enabled: open && q.length >= 2,
  });
  return (
    <>
      <Button
        variant="ghost"
        size="icon"
        aria-label={t("chat.search._")}
        onClick={() => setOpen(true)}
      >
        <HugeiconsIcon icon={Search01Icon} />
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-h-[85dvh] overflow-hidden sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{t("chat.search._")}</DialogTitle>
            <DialogDescription className="sr-only">
              {conversation.title}
            </DialogDescription>
          </DialogHeader>
          <Input
            autoFocus
            value={term}
            placeholder={t("chat.search.placeholder")}
            aria-label={t("chat.search.placeholder")}
            onChange={(event) => setTerm(event.target.value)}
          />
          <ul className="-mx-2 min-h-0 overflow-y-auto">
            {query.data?.length === 0 && (
              <li className="px-2 py-3 text-sm text-muted-foreground">
                {t("chat.search.noResults")}
              </li>
            )}
            {query.data?.map((message) => (
              <li key={message.id}>
                <button
                  type="button"
                  className="w-full rounded-md px-2 py-1.5 text-left hover:bg-muted"
                  onClick={() => {
                    setOpen(false);
                    onJump(message);
                  }}
                >
                  <span className="text-xs text-muted-foreground">
                    {message.author.displayName},{" "}
                    {formatTimestampMinutes(message.created, formatContext)}
                  </span>
                  <span className="line-clamp-2 block text-sm">
                    {richTextToPlainText(message.content ?? "")}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </DialogContent>
      </Dialog>
    </>
  );
}
