"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon, QuoteDownIcon } from "@hugeicons/core-free-icons";
import { richTextToPlainText } from "@/components/shared/rich-text";
import { RefName } from "@/components/shared/ref-list";
import { Button } from "@/components/ui/button";
import type { ChatMessage } from "@/lib/rs/chat-types";

/** The message quoted by the one being written (MSG-08): author and the beginning of its text, removable. */
export function QuoteBar({
  message,
  onRemove,
}: {
  message: ChatMessage;
  onRemove: () => void;
}) {
  const t = useTranslations();
  const text = richTextToPlainText(message.content ?? "");
  return (
    <div className="flex items-center gap-2 px-2 pt-2">
      <HugeiconsIcon
        icon={QuoteDownIcon}
        className="size-4 shrink-0 text-muted-foreground"
      />
      <div className="min-w-0 flex-1 border-l-2 border-primary pl-2 text-xs">
        <div className="font-semibold">
          <RefName value={message.author} />
        </div>
        <div className="truncate text-muted-foreground">{text}</div>
      </div>
      <Button
        type="button"
        variant="ghost"
        size="icon-xs"
        aria-label={t("chat.message.quote.remove")}
        onClick={onRemove}
      >
        <HugeiconsIcon icon={Cancel01Icon} />
      </Button>
    </div>
  );
}
