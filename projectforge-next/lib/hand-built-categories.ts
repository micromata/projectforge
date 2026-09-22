/**
 * Categories that have a hand built page in this app. They own concrete routes
 * (`book`, `book/[id]`), which Next resolves before the generic `[category]`
 * catch-alls — so reaching one of those with such a category means the url was
 * wrong, not that the generic renderer should take over.
 *
 * Keep in sync with `NextMigration.MIGRATED` in projectforge-business: a category
 * is either hand built (listed here) or server-laid-out, never both (asserted by
 * `NextMigrationTest`, which parses this array). One entry per category, and the
 * REST category is what a category is keyed by here — usually also its route
 * (`/next/book`, `/rs/book`), but not always: the outgoing invoice is served under
 * `/next/invoice`, because that is what the entity is called (see
 * `NextMigration.MIGRATED["outgoingInvoice"]`).
 */
export const HAND_BUILT_CATEGORIES = [
  "book",
  "calendar",
  "cost1",
  "group",
  "incomingInvoice",
  "order",
  "outgoingInvoice",
  "task",
  "teamEvent",
  "timesheet",
];

export function isHandBuilt(category: string | undefined): boolean {
  return !!category && HAND_BUILT_CATEGORIES.includes(category);
}
