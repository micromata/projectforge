"use client";

import { useListMeta } from "./use-list-meta";

/**
 * Whether this user may change entries of the entity — the entity-wide flag the backend reports as
 * `listMeta.userAccess.update` (`AbstractEntityRest.listUpdateAccess`, default `true`).
 *
 * The counterpart of [useInsertAccess]: a right on the entity, not on a single entry (for that,
 * `GET /rs/{entity}/{id}` answers `writeAccess`, see lib/rs/entity-access.ts). A cache read, not a
 * second call — the list page loads `listMeta` for its filter fields anyway.
 *
 * Used to hide write-only affordances from a read-only viewer: the outgoing invoice list overrides
 * `listUpdateAccess()` to `false` for order-book users, which drops the mass-update toggle and the
 * cost-assignment export for them while leaving them for finance/controlling.
 *
 * Undefined while it isn't loaded yet, so a caller can tell "not allowed" from "not known yet".
 */
export function useUpdateAccess(entity: string): boolean | undefined {
  return useListMeta(entity).data?.userAccess?.update;
}
