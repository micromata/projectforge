"use client";

import { useInitialList } from "./use-initial-list";

/**
 * The legacy React edit page of one entry, for the escape hatch of a hand built edit page
 * (see LegacyPageLink).
 *
 * The server-laid-out pages read `ui.legacyUrl` straight from their own response; a hand built page
 * has no such response, so it takes the template (`react/book/edit/:id`) from the list layout, which
 * it already loads for the filter fields — this is a cache read, not a second call.
 *
 * @param id null while adding an entry: the legacy add page is meant then, and that is what the
 *   template without an id resolves to.
 */
export function useLegacyEditUrl(
  entity: string,
  id: number | null
): string | undefined {
  const template = useInitialList(entity).data?.legacyEditPage;
  if (!template) return undefined;
  return id == null
    ? template.replace(/\/:id$/, "")
    : template.replace(":id", String(id));
}
