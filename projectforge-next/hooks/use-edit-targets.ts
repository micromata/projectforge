"use client";

import { useRouter } from "next/navigation";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { useListMeta } from "./use-list-meta";

/** Where the add button leads and what a row click opens, for one list page. */
export interface EditTargets {
  /** Target of the add button, and of the "new entry" shortcut. */
  addHref: string;
  /** Opens the entry with that id — a route of this app, or a full load of the legacy page. */
  openEntry: (id: number) => void;
  /**
   * Whether the two leave this app. The add button is a `next/link` either way, so this is what tells
   * it to do a full page load (see LegacyPageLink for why that is unavoidable).
   */
  legacy: boolean;
}

/**
 * The two ways out of a list — "add" and a row click — resolved from whether the page has a form here.
 *
 * A page whose list is migrated but whose form is not (`PageDef.edit` absent) still has to open its
 * entries: it points at the legacy page the backend names for that entity (`listMeta.legacyEditPage`
 * / `legacyNewEntryPage`) rather than at a route of this app that does not exist. Which page that is
 * cannot be derived here — Wicket carries the id as a query parameter, the legacy React app as a path
 * segment — so the template comes from the server, with `:id` where the id goes (see
 * NextMigration.legacyEditPage and useLegacyEditUrl, which does the same for an edit page's own
 * escape hatch).
 *
 * Both targets in one hook because they are one decision: a list that opens the legacy form must add
 * there too, or the user would land in a form this app cannot save.
 *
 * @param hasEditPage Whether this app renders the form, i.e. `page.edit` is declared.
 */
export function useEditTargets(
  entity: string,
  route: string,
  hasEditPage: boolean
): EditTargets {
  const router = useRouter();
  // Read unconditionally: `listMeta` is loaded for the filter fields anyway, so this is a cache read,
  // and a hook may not be called conditionally.
  const meta = useListMeta(entity).data;

  if (hasEditPage) {
    return {
      addHref: `${route}/new`,
      openEntry: (id) => router.push(`${route}/${id}`),
      legacy: false,
    };
  }

  const legacyUrl = (template: string | undefined, id?: number) =>
    template
      ? toAbsoluteUrl(
          resolveMenuUrl(
            id == null ? template : template.replace(":id", String(id))
          )
        )
      : undefined;

  return {
    // `#` while the meta is still loading: the button stays reachable and its shortcut does nothing,
    // rather than the toolbar rendering without it and jumping once the url arrives.
    addHref: legacyUrl(meta?.legacyNewEntryPage) ?? "#",
    openEntry: (id) => {
      const url = legacyUrl(meta?.legacyEditPage, id);
      if (url) window.location.href = url;
    },
    legacy: true,
  };
}
