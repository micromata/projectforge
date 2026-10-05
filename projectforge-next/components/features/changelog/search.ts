import type {
  ChangelogItem,
  ChangelogNews,
  ChangelogRelease,
  ChangelogSection,
} from "./types";

/**
 * The full text search of the changelog page, over everything a release or news holds — collapsed or
 * not. The search string is split into words, and a text matches if it contains all of them, case
 * insensitive. Markup of the texts (`**`, `{red}`, link targets) is searched as it is; it never gets in
 * the way of a word.
 */
export function searchTerms(query: string): string[] {
  return query.toLowerCase().split(/\s+/).filter(Boolean);
}

function matches(texts: (string | undefined)[], terms: string[]): boolean {
  const haystack = texts.filter(Boolean).join("\n").toLowerCase();
  return terms.every((term) => haystack.includes(term));
}

/** The item if it matches together with its [context]; a group keeps only its matching items. */
function filterItem(
  item: ChangelogItem,
  terms: string[],
  context: (string | undefined)[]
): ChangelogItem | null {
  if (typeof item === "string")
    return matches([...context, item], terms) ? item : null;
  const groupContext = [...context, item.title];
  if (matches(groupContext, terms)) return item;
  const items = item.items.filter((child) =>
    matches([...groupContext, child], terms)
  );
  return items.length > 0 ? { ...item, items } : null;
}

/**
 * The release reduced to its matching items, or null if nothing in it matches. An item matches if the
 * words are found in it together with what it stands under: the release (title, version, date, intro,
 * tag, commits), the section type and the group title — so "fixed ldap" finds the LDAP fixes, and a
 * match of the release alone keeps all its items.
 */
export function filterRelease(
  release: ChangelogRelease,
  terms: string[]
): ChangelogRelease | null {
  if (terms.length === 0) return release;
  const context = [
    release.title,
    release.version,
    release.date,
    release.tag,
    release.fromCommit,
    release.toCommit,
    ...(release.intro ?? []),
  ];
  const sections = release.sections
    .map((section): ChangelogSection | null => {
      const items = section.items
        .map((item) => filterItem(item, terms, [...context, section.type]))
        .filter((item): item is ChangelogItem => item !== null);
      return items.length > 0 ? { ...section, items } : null;
    })
    .filter((section): section is ChangelogSection => section !== null);
  return sections.length > 0 || matches(context, terms)
    ? { ...release, sections }
    : null;
}

export function newsMatches(news: ChangelogNews, terms: string[]): boolean {
  return (
    terms.length === 0 ||
    matches(
      [
        news.title,
        news.version,
        news.date,
        news.text,
        ...(news.highlights ?? []),
      ],
      terms
    )
  );
}
