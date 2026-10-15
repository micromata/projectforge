"use client";

import type { WizardGroups } from "./types";

/**
 * The groups the wizard had already picked, on their way through the detour to `/task/new`.
 *
 * Same pattern and same reasoning as [usePendingClone]: this app is a static export, so no state
 * rides along a route change, and the url carries only the *fact* that something was created
 * (`?highlightId=42`, see EditReturn.savedRoute) while the payload waits in a module variable.
 *
 * Reading is idempotent — the wizard can mount more than once for one navigation (a Suspense retry,
 * React's double invocation in development) — so nothing is consumed here. What ends it is the next
 * detour, which replaces the value, and a full page load, which drops it with the rest of the JS: the
 * wizard then starts empty, which is the harmless of the two outcomes.
 */
let stashed: WizardGroups | null = null;

/** Remembers what is picked before leaving the wizard to create a structure element. */
export function stashWizardGroups(groups: WizardGroups): void {
  stashed = groups;
}

/** What was picked before the detour, or an empty selection for a wizard opened fresh. */
export function takeWizardGroups(): WizardGroups {
  return stashed ?? {};
}
