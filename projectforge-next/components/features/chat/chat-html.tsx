"use client";

import { useMemo } from "react";
import { sanitizeChatHtml } from "@/components/shared/rich-text";
import { cn } from "@/lib/utils";

/** The content of a chat message, sanitized ([sanitizeChatHtml]), with the mentions highlighted. */
export function ChatHtml({
  html,
  className,
}: {
  html: string;
  className?: string;
}) {
  const sanitized = useMemo(() => sanitizeChatHtml(html), [html]);
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
