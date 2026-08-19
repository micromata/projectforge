/**
 * The routes of the task pages, spelled once.
 *
 * Here rather than beside the page declaration, because the tree is shared chrome (it is the body of
 * every task select field as well) and has to link to the edit page without importing a feature — see
 * the tier rules in projectforge-next/CLAUDE.md. The declaration imports these instead.
 */

/** Route of the structure tree, the page a task is reached from today. */
export const TASK_TREE_ROUTE = "/taskTree";

/** Route the task pages live under; `${TASK_ROUTE}/${id}` is one task's form. */
export const TASK_ROUTE = "/task";

/**
 * The url of a task's edit page, with the page to return to from it.
 *
 * `returnTo` is a whitelist the declaration owns (`EditDef.returnTargets`), so a value it doesn't name
 * is ignored rather than followed — see useEditReturn.
 */
export function taskHref(id: number, options?: { returnTo?: string }): string {
  return `${TASK_ROUTE}/${id}${query(options)}`;
}

/**
 * The url that adds a task.
 *
 * `parentTaskId` is not a value of the form but a parameter of the *preset*: the backend resolves the
 * parent (and with it the project the cost unit block needs) and answers `{entity}/newEntry` with a
 * filled task — see `TaskPagesRest.newBaseDO` and useNewEntryParams. Left out, the new task hangs below
 * the root, which is what the tree's own "add" button means.
 */
export function newTaskHref(options?: {
  parentTaskId?: number;
  returnTo?: string;
}): string {
  return `${TASK_ROUTE}/new${query(options)}`;
}

/** The parameters that are actually set, encoded — an empty object yields no `?`. */
function query(options?: Record<string, string | number | undefined>): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(options ?? {})) {
    if (value !== undefined) params.set(key, String(value));
  }
  return params.size > 0 ? `?${params}` : "";
}
