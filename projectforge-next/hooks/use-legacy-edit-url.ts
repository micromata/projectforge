"use client";

import { useInitialList } from "./use-initial-list";

/**
 * The legacy edit page of one entry, for the escape hatch of a hand built edit page
 * (see LegacyPageLink).
 *
 * The server-laid-out pages read `ui.legacyUrl` straight from their own response; a hand built page
 * has no such response, so it takes the template (`react/book/edit/:id`, `wa/cost1Edit?id=:id`) from
 * the list layout, which it already loads for the filter fields — this is a cache read, not a second
 * call.
 *
 * The add page comes from the server too, because it isn't derivable from the template: the legacy
 * page may live in the React app or in Wicket, which carries the id as a query parameter rather than
 * a path segment (see NextMigration.LegacyApp).
 *
 * @param id null while adding an entry: the legacy add page is meant then.
 */
export function useLegacyEditUrl(
  entity: string,
  id: number | null
): string | undefined {
  const data = useInitialList(entity).data;
  if (id == null) return data?.legacyNewEntryPage;
  return data?.legacyEditPage?.replace(":id", String(id));
}
