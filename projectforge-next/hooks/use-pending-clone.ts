"use client";

import { useSearchParams } from "next/navigation";

/**
 * The clone a user just asked for, on its way from the entry it was made from to the add page.
 *
 * It has to travel *outside* React and outside the URL: this app is a static export, so no state
 * rides along a route change, and the clone is a whole entity — dozens of fields, its positions and
 * their cost splits — which no query string could carry. A module variable rather than the query
 * cache, because a clone must not become the starting point of the *next* plain "add": seeded into
 * `[entity, null]` it would still be there for it.
 *
 * What the URL does carry is the fact that this add page is a clone (`?clone=1`), and that is what
 * decides whether the value below is read. Reading must be idempotent: the add page can mount more
 * than once for one navigation (a Suspense retry re-runs it, and so does React's double invocation in
 * development), and a handover that clears itself on the first read leaves every later mount with the
 * backend's preset instead — an empty form. So nothing is consumed here; the value is simply ignored
 * without the parameter, and replaced by the next clone.
 *
 * Keyed by entity, one entry at most: a clone is handed over within a single navigation, so a second
 * one can only mean the first was abandoned.
 */
let pending: { entity: string; data: unknown } | null = null;

/** Marks an add page as opened by the clone button — see usePendingClone. */
export const CLONE_PARAM = "clone";

/** Remembers the clone for the add page of this entity, replacing an abandoned one. */
export function setPendingClone(entity: string, data: unknown): void {
  pending = { entity, data };
}

/**
 * The clone this add page was opened with, or undefined for a plain "add".
 *
 * A full page load (a reload of `…/new?clone=1`, or the link pasted somewhere) drops the module
 * variable with the rest of the JS, so the form starts from the backend's preset — the same as any
 * other add, which is the safe end of the two.
 *
 * @param enabled false on the edit page of a stored entry: there is nothing to take there.
 */
export function usePendingClone<D>(
  entity: string,
  enabled: boolean
): D | undefined {
  const isClone = useSearchParams().get(CLONE_PARAM) === "1";
  if (!enabled || !isClone || pending?.entity !== entity) return undefined;

  return pending.data as D;
}
