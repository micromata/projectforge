"use client";

import { useState } from "react";
import { useAuth } from "./use-auth";

/**
 * How a picker deals with inactive entries (deactivated users, employees who left, ended projects,
 * customers and cost units):
 * - `toggle` (default): hidden, with a "show inactive" checkbox for those who may see them;
 * - `hidden`: hidden, no checkbox;
 * - `shown`: always asked for (the backend still drops them for users who may not see them).
 *
 * An entity without an inactive state ignores it.
 */
export type InactiveMode = "toggle" | "hidden" | "shown";

/** The `autosearch` request parameter, see `AutoCompletion.SHOW_INACTIVE_PARAM`. */
export const SHOW_INACTIVE_PARAM = "showInactive";

/**
 * The entities whose `autosearch` hides inactive entries unless asked for, and whether only the members
 * of `AutoCompletion.SHOW_INACTIVE_PERSONS_GROUPS` may ask (`UserStatus.showInactivePersons`).
 */
const INACTIVE_ENTITIES: Record<string, { personal: boolean }> = {
  user: { personal: true },
  employee: { personal: true },
  cost1: { personal: false },
  cost2: { personal: false },
  project: { personal: false },
  customer: { personal: false },
};

/** The entity of an `autosearch` url (`user/autosearch?search=:search` → `user`), null for any other url. */
export function autosearchEntity(url: string): string | null {
  return /^\/?([^/?]+)\/autosearch\b/.exec(url)?.[1] ?? null;
}

const storageKey = (entity: string) => `pf.showInactive.${entity}`;

function readStored(entity: string): boolean {
  try {
    return window.localStorage.getItem(storageKey(entity)) === "true";
  } catch {
    return false;
  }
}

/**
 * Whether a picker of the given `autosearch` url offers the "show inactive" checkbox, its state, and the
 * request parameters following from it. The state is remembered per entity in the local storage, so the
 * next picker of the same entity starts the same way. Read once on mount: the search list mounts fresh
 * each time its popover opens.
 */
export function useShowInactive(url: string, mode: InactiveMode = "toggle") {
  const { mayShowInactivePersons } = useAuth();
  const entity = autosearchEntity(url);
  const config = entity ? INACTIVE_ENTITIES[entity] : undefined;
  const allowed = !!config && (!config.personal || mayShowInactivePersons);
  const [checked, setCheckedState] = useState(() =>
    entity && mode === "toggle" ? readStored(entity) : false
  );

  const setChecked = (value: boolean) => {
    setCheckedState(value);
    if (!entity) return;
    try {
      window.localStorage.setItem(storageKey(entity), String(value));
    } catch {
      // Private mode or full storage: the state just isn't remembered.
    }
  };

  const showInactive =
    allowed && (mode === "shown" || (mode === "toggle" && checked));
  return {
    /** Whether to render the checkbox. */
    toggleable: allowed && mode === "toggle",
    checked,
    setChecked,
    /** Extra request parameters for the lookup, null when there is nothing to add. */
    params: showInactive ? { [SHOW_INACTIVE_PARAM]: true } : null,
  };
}
