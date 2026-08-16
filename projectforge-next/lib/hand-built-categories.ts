/**
 * Categories that have a hand built page in this app. They own concrete routes
 * (`book`, `book/[id]`), which Next resolves before the generic `[category]`
 * catch-alls — so reaching one of those with such a category means the url was
 * wrong, not that the generic renderer should take over.
 *
 * Keep in sync with `NextMigration.MIGRATED` in projectforge-business: a category
 * is either hand built (listed here) or server-laid-out, never both (asserted by
 * `NextMigrationTest`, which parses this array). One entry per
 * category — the route of a hand built page is its REST category, so the url reads
 * the same either way (`/next/book`, `/rs/book`).
 */
export const HAND_BUILT_CATEGORIES = ["book", "cost1", "order"];

export function isHandBuilt(category: string | undefined): boolean {
  return !!category && HAND_BUILT_CATEGORIES.includes(category);
}
