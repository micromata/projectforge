"use client";

import { useMemo } from "react";
import { HighlightedText } from "@/components/shared/highlighted-text";
import {
  markdownToRichText,
  richTextToPlainText,
} from "@/components/shared/rich-text";
import { cn } from "@/lib/utils";

/**
 * A rich text (see [RichText]) as the plain text of a list cell: the markup dropped, the blocks joined by
 * a space, at most two lines, the search term highlighted. With `markdown`, a value written before it was
 * rich text loses its Markdown syntax the same way (see [markdownToRichText]).
 */
export function RichTextPreview({
  text,
  markdown = false,
  highlight,
  className,
}: {
  text: string | null | undefined;
  markdown?: boolean;
  highlight?: string;
  className?: string;
}) {
  const plain = useMemo(
    () =>
      text
        ? richTextToPlainText(markdown ? markdownToRichText(text) : text)
        : "",
    [text, markdown]
  );
  if (!plain) return null;
  return (
    <span className={cn("line-clamp-2", className)}>
      <HighlightedText text={plain} query={highlight} />
    </span>
  );
}
