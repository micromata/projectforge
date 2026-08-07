/**
 * Categories that have a hand built page in this app. They own concrete routes
 * (`books`, `books/[id]`), which Next resolves before the generic `[category]`
 * catch-alls — so reaching one of those with such a category means the url was
 * wrong, not that the generic renderer should take over.
 *
 * Keep in sync with `NextMigration.MIGRATED` in projectforge-business: a category
 * is either hand built (listed here) or server-laid-out, never both. Both the
 * REST category and the next route are listed, because the url may carry either.
 */
export const HAND_BUILT_CATEGORIES = ["book", "books"];

export function isHandBuilt(category: string | undefined): boolean {
  return !!category && HAND_BUILT_CATEGORIES.includes(category);
}
