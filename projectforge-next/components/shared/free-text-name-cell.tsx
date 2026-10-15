"use client";

import { HighlightedText } from "@/components/shared/highlighted-text";
import { FreeTextBadge } from "./free-text-badge";

/**
 * A list cell showing a name that is either a record's or a typed free text — the customer column of the
 * order book and of the invoice list. A free text carries the [FreeTextBadge] and is set in italics, the
 * same marking [EntityOrTextAutocomplete] gives it in the form.
 */
export function FreeTextNameCell({
  name,
  freeText,
  highlight,
}: {
  name?: string | null;
  /** Whether [name] is a free text rather than a record's name. */
  freeText: boolean;
  /** The active search term, see HighlightedText. */
  highlight?: string;
}) {
  if (!name) return null;
  if (!freeText) return <HighlightedText text={name} query={highlight} />;
  return (
    <span className="flex min-w-0 items-center gap-1.5">
      <FreeTextBadge />
      {/* `pr-0.5`: an italic glyph leans past its box, which `truncate` would clip. */}
      <span className="truncate pr-0.5 italic">
        <HighlightedText text={name} query={highlight} />
      </span>
    </span>
  );
}
