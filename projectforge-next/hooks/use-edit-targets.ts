"use client";

import { useRouter } from "next/navigation";
import { navigateInGesture } from "@/lib/navigate-in-gesture";
import { useListMeta } from "./use-list-meta";

/** Where the add button leads and what a row click opens, for one list page. */
export interface EditTargets {
  /** Target of the add button, and of the "new entry" shortcut. */
  addHref: string;
  /** Opens the entry with that id — a route of this app. */
  openEntry: (id: number) => void;
  /**
   * Whether this user may add an entry at all — false leaves the button (and with it its shortcut) out
   * of the toolbar, as the legacy list page left the create entry out of its menu.
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
   * users answers false here (`AbstractEntityRest.listUpdateAccess`, overridden by `GroupEntityRest`), and
   * then a row that opens a form nobody may save is a dead end worth not offering.
   */
  canOpen: boolean;
}

/**
 * The two ways out of a list — "add" and a row click — resolved from whether the page has a form here.
 *
 * A page without a form in this app (`PageDef.edit` absent) offers neither: no add button, no row click.
 *
 * Both targets in one hook because they are one decision.
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
  // the legacy list layout used (`if (ui.userAccess.insert != false)`). It also covers the
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
      // Within the row click, so that Safari keeps the entry for back (see navigateInGesture).
      openEntry: (id) => navigateInGesture(router, `${route}/${id}${back}`),
    };
  }

  // No form in this app: nothing to add and nothing to open.
  return {
    canAdd: false,
    canOpen: false,
    addHref: "#",
    openEntry: () => {},
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
