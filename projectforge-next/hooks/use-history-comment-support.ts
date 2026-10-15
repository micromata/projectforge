"use client";

import { useListMeta } from "./use-list-meta";

/**
 * Whether a write of this entity may carry a comment for the history entry it produces — the
 * „Änderungskommentar" of the edit page.
 *
 * A property of the entity, not of the entry or of the user: the DAO answers it from the DO
 * (`BaseDao.supportsHistoryUserComments`, true for whatever implements `HistoryUserCommentSupport`),
 * and the backend reports it as `listMeta.userAccess.editHistoryComments` — the same flag the server
 * laid out pages read before adding their comment field (`LayoutUtils.processEditPage`).
 *
 * A cache read, not a second call: the edit page loads `listMeta` for the legacy url and the insert
 * right anyway (see useInsertAccess).
 */
export function useHistoryCommentSupport(entity: string): boolean {
  return useListMeta(entity).data?.userAccess?.editHistoryComments === true;
}
