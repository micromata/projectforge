/**
 * Links into the (migrated) access-rights list, spelled once.
 *
 * Here rather than beside the access page declaration, because the caller — the task form's "show access
 * rights" cross-link — may not import the access feature (see the tier rules in projectforge-next/CLAUDE.md).
 * Pure url building, so it belongs in lib (as lib/timesheet-links.ts).
 */

/**
 * Query parameters a link carries into the access list: the structure element to filter by, and its name
 * for the filter pill. The access list route reads them back (see app/(authenticated)/access/page.tsx) and
 * seeds a transient filter — what Wicket's `AccessListPage` did with its `taskId` page parameter.
 */
export const ACCESS_TASK_ID_PARAM = "taskId";
export const ACCESS_TASK_NAME_PARAM = "taskName";

/** The access rights granted on one structure element. */
export function accessListHref(taskId: number, taskName?: string): string {
  const params = new URLSearchParams({
    [ACCESS_TASK_ID_PARAM]: String(taskId),
  });
  if (taskName) params.set(ACCESS_TASK_NAME_PARAM, taskName);
  return `next/access?${params.toString()}`;
}
