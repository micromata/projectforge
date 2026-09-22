"use client";

import { useListMeta } from "./use-list-meta";

/**
 * Whether this user may add an entry of the entity.
 *
 * Not derivable from the entry itself: `GET /rs/{entity}/{id}` answers `writeAccess`/`deleteAccess`
 * for *that* entry (see lib/rs/entity-access.ts), while inserting is a right on the entity, which
 * the backend reports as `listMeta.userAccess.insert`. A cache read, not a second call — the list
 * page loads `listMeta` for its filter fields anyway (same as useLegacyEditUrl).
 *
 * Undefined while it isn't loaded yet, so a caller can tell "not allowed" from "not known yet".
 */
export function useInsertAccess(entity: string): boolean | undefined {
  return useListMeta(entity).data?.userAccess?.insert;
}
