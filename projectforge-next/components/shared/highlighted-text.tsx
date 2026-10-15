import { Fragment } from "react";
import { cn } from "@/lib/utils";

/** Escapes the regex metacharacters in a user term, so a `.` matches itself. */
const REGEX_SPECIALS = /[.*+?^${}()|[\]\\]/g;

/**
 * Builds one case-insensitive alternation regex from the term's words, or `null` when it holds
 * nothing to match.
 *
 * The words are matched independently, so a multi-word list search ("meyer berlin") lights up each
 * word wherever it sits across the row's columns, not only an exact phrase. MagicFilter's decoration
 * around a word is undone so the stem still matches: a leading `+` (the word is required) is dropped,
 * surrounding quotes and the `*` wildcard are stripped. A word negated with a leading `-` is excluded
 * from the results, so there is nothing of it to highlight — it is left out. A leftover operator (a
 * bare `field:value`) simply finds nothing, which is safe.
 */
function highlightPattern(query: string): RegExp | null {
  const terms = query
    .trim()
    .split(/\s+/)
    .filter((term) => !term.startsWith("-"))
    .map((term) => term.replace(/^\+/, "").replace(/^["'*]+|["'*]+$/g, ""))
    .filter((term) => term.length > 0)
    .map((term) => term.replace(REGEX_SPECIALS, "\\$&"));
  if (terms.length === 0) return null;
  return new RegExp(`(${terms.join("|")})`, "gi");
}

/**
 * Renders `text` with every match of `query` wrapped in a `<mark>` — the visual cue where a search
 * term matched. Case-insensitive, plain substring per word, mirroring the `containsIgnoreCase` /
 * `.includes` matching of the surfaces that use it (task tree, entity lists, quick access, recent
 * time sheets, option combobox), never a fuzzy match.
 *
 * With no term it renders the text untouched, so a caller can pass it unconditionally.
 */
export function HighlightedText({
  text,
  query,
  className,
}: {
  text: string;
  /** The active search term; when empty or whitespace, nothing is highlighted. */
  query?: string;
  /** Extra classes on each `<mark>`, on top of the shared `.text-match` styling. */
  className?: string;
}) {
  const pattern = query ? highlightPattern(query) : null;
  if (!pattern) return <>{text}</>;
  // The capturing group keeps the matches in the split result: every odd index is one of them, with
  // the original casing preserved (the matched text, not the lower-cased term).
  const parts = text.split(pattern);
  return (
    <>
      {parts.map((part, index) =>
        index % 2 === 1 ? (
          <mark
            key={`${index}:${part}`}
            className={cn("text-match", className)}
          >
            {part}
          </mark>
        ) : (
          <Fragment key={`${index}:${part}`}>{part}</Fragment>
        )
      )}
    </>
  );
}
