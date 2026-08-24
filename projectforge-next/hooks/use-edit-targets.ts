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
  /**
   * Whether this user may add an entry at all — false leaves the button (and with it its shortcut) out
   * of the toolbar, as `AbstractPagesRest.createListLayout` leaves the create entry out of the legacy
   * page's menu.
   */
  canAdd: boolean;
  /**
   * Whether a row may be opened at all — false leaves the list without its row click, as Wicket's list
   * renders a plain label instead of a link (`GroupListPage`) and as the server laid out grid drops its
   * `clickable` (`AGGridSupport`).
   *
   * The entity wide question, not the entry's: for most entities the backend answers it with true and
   * whether *this* entry may be written travels on its DTO (`writeAccess`, see lib/rs/entity-access.ts),
   * which is what makes the form read-only. Only an entity that refuses every write to a whole class of
   * users answers false here (`AbstractEntityRest.listUpdateAccess`, overridden by `GroupPagesRest`), and
   * then a row that opens a form nobody may save is a dead end worth not offering.
   */
  canOpen: boolean;
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
 * @param returnTargets The form's declared callers (`EditDef.returnTargets`), so a list that is one
 *   of them can name itself in the url it opens — see [returnToQuery].
 */
export function useEditTargets(
  entity: string,
  route: string,
  hasEditPage: boolean,
  returnTargets?: { route: string }[]
): EditTargets {
  const router = useRouter();
  // Read unconditionally: `listMeta` is loaded for the filter fields anyway, so this is a cache read,
  // and a hook may not be called conditionally.
  const meta = useListMeta(entity).data;
  // `!== false` rather than `=== true`: a flag the entity's rest class fills with nothing means "not
  // restricted" — the same reading `entityAccess` gives a missing flag of an entry, and the one
  // `AbstractPagesRest` writes down itself (`if (ui.userAccess.insert != false)`). It also covers the
  // moment before `listMeta` is there, which is no moment for a hand-built list: its shell waits for
  // that response anyway (see useReadAccessGuard).
  const access = {
    canAdd: meta?.userAccess?.insert !== false,
    canOpen: meta?.userAccess?.update !== false,
  };

  if (hasEditPage) {
    const back = returnToQuery(route, returnTargets);
    return {
      ...access,
      addHref: `${route}/new${back}`,
      openEntry: (id) => router.push(`${route}/${id}${back}`),
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

  // The right applies to the legacy target as well: a list that hands its entries to Wicket must not
  // offer an add page that refuses them there.
  return {
    ...access,
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

/**
 * `?returnTo=<the list>`, for a form that names more than one caller — and nothing otherwise.
 *
 * A form whose `returnTargets` are absent returns to its entity's own list anyway, so the parameter
 * would say what the default already says. A form that has them, though, has a *first* one as its
 * default, and that need not be this list: a task's form is reached from the structure tree as well,
 * and without the parameter a row opened from the list would send the user to the tree on cancel.
 *
 * Only a route the form itself named is passed on. That is not sanitizing — `useEditReturn` ignores an
 * unknown value regardless — but it keeps a page out of the url that the form would silently drop.
 */
function returnToQuery(route: string, targets?: { route: string }[]): string {
  if (!targets?.some((target) => target.route === route)) return "";
  return `?returnTo=${encodeURIComponent(route)}`;
}
