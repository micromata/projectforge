import type { ReactNode } from "react";

/**
 * The words of a search, as the backend reads them (`SearchTerms`): `"…"` quotes a phrase; a word with a leading
 * `-` is excluded and so not part of a hit (not highlighted).
 */
export function searchTerms(search: string | null | undefined): string[] {
  const terms: string[] = [];
  for (const match of (search ?? "").matchAll(/(-?)(?:"([^"]*)"?|(\S+))/g)) {
    const term = match[2] || match[3] || "";
    if (term && term !== "-" && !match[1]) terms.push(term);
  }
  return terms;
}

/** The text with the terms marked, case-insensitive. */
export function highlightTerms(text: string, terms: string[]): ReactNode {
  if (terms.length === 0 || !text) return text;
  const escaped = terms.map((term) =>
    term.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
  );
  // A capturing group: split puts the matches at the odd indices.
  return text
    .split(new RegExp(`(${escaped.join("|")})`, "gi"))
    .map((part, index) =>
      index % 2 === 1 ? (
        <mark
          key={index}
          className="rounded-sm bg-yellow-200 text-inherit dark:bg-yellow-600/50"
        >
          {part}
        </mark>
      ) : (
        part
      )
    );
}
