"use client";

import { MarkdownText } from "@/components/shared/markdown-text";

/**
 * A text of the changelog: the markdown subset of changelog/changelog.json with its one extension
 * `{red}…{/red}` turned into a span. Safe as raw HTML: the texts are authored in the repository and the
 * generator rejects any HTML in them.
 */
export function ChangelogText({
  text,
  className,
}: {
  text: string;
  className?: string;
}) {
  const html = text.replace(
    /\{red\}(.*?)\{\/red\}/g,
    '<span class="text-destructive">$1</span>'
  );
  return <MarkdownText text={html} allowHtml className={className} />;
}
