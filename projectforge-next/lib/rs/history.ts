/**
 * Change history of an entity — the same endpoints for every `AbstractPagesRest` page.
 *
 * Reading goes through `request()` (plain JSON), appending a comment through the UILayout modal's
 * endpoint, which speaks the `PostData`/`ResponseAction` protocol (see ./entity.ts). The CSRF token
 * is not passed here: `rawRequest` sets the header for every state changing call.
 */

import { rawRequest, request, RsError } from "./client";

/** org.projectforge.framework.persistence.history.EntityOpType. */
export type EntityOpType = "Insert" | "Update" | "Delete";
/** org.projectforge.framework.persistence.history.PropertyOpType. */
export type PropertyOpType = "Insert" | "Update" | "Delete";

/** One changed property of an entry, see DisplayHistoryEntryAttr. */
export interface HistoryEntryAttr {
  id: number | null;
  propertyName: string | null;
  /** Translated field name; falls back to `propertyName` when the backend knows none. */
  displayPropertyName: string | null;
  operationType: PropertyOpType | null;
  /** Translated name of `operationType`. */
  operation: string | null;
  oldValue: string | null;
  newValue: string | null;
}

/** How many properties an entry inserted/updated/deleted, see DisplayHistoryEntry.DiffCount. */
export interface HistoryDiffCount {
  type: EntityOpType;
  count: number;
  /** Translated operation name, e.g. "geändert". */
  operation: string;
}

/** org.projectforge.framework.persistence.history.DisplayHistoryEntry. */
export interface HistoryEntry {
  /** Primary key in t_pf_history — what a comment is appended to. */
  id: number;
  modifiedAt: string;
  /** Localized "3 days ago", built by the backend. */
  timeAgo: string;
  modifiedByUserId: number | null;
  modifiedByUser: string | null;
  operationType: EntityOpType;
  /** Translated name of `operationType`. */
  operation: string;
  /** Change comments, one timestamped line each (HistoryService.appendUserComment). */
  userComment: string | null;
  diffSummary: HistoryDiffCount[];
  attributes: HistoryEntryAttr[];
}

/** AbstractPagesRest.HistoryInfo: the entries plus what the client may do with them. */
export interface HistoryInfo {
  entries: HistoryEntry[];
  /**
   * Whether comments may be appended (`BaseDao.supportsHistoryUserComments`). Only entities
   * implementing `HistoryUserCommentSupport` — e.g. user, group — do.
   */
  supportsUserComments: boolean;
}

export function fetchHistory(
  entity: string,
  id: number,
  signal?: AbortSignal
): Promise<HistoryInfo> {
  return request<HistoryInfo>(
    `/rs/${entity}/history/${id}`,
    { method: "GET" },
    signal
  );
}

/**
 * Appends a comment to one history entry.
 *
 * The endpoint belongs to the UILayout modal of the legacy frontend, hence the `PostData` envelope
 * and the `ResponseAction` answer (`CLOSE_MODAL`), which carries nothing this client needs.
 * Comments are append-only: the backend prefixes each with a timestamp and the user's name.
 */
export async function appendHistoryComment(
  entryId: number,
  comment: string,
  signal?: AbortSignal
): Promise<void> {
  const path = "/rs/historyEntries/append";
  const res = await rawRequest(
    path,
    {
      method: "PUT",
      body: JSON.stringify({ data: { id: entryId, appendComment: comment } }),
    },
    signal
  );
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
}
