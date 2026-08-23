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
 * Route of the structure wizard, the page that grants a set of groups their rights on one element.
 *
 * Here and not beside the feature, because both the tree's action bar (shared chrome) and the task
 * form's `returnTargets` name it — the wizard sends the user to the form to add an element and expects
 * them back.
 */
export const TASK_WIZARD_ROUTE = "/taskWizard";

/**
 * The parameter a caller of the task form gets the id of the saved element back in — set by the form
 * (`EditDef.returnTargets`, see useEditReturn) and read by the page that returns.
 *
 * Two callers ask for it: the wizard goes on with the element that was just created, and the tree
 * marks it (Wicket's `PARAMETER_HIGHLIGHTED_ROW`). Beside the routes for the same reason they are
 * here — the form's declaration and both pages need it, and none of them may import the other.
 */
export const SAVED_ID_PARAM = "savedId";

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
 * parent (and with it the project the cost unit block needs, and the rights the finance section is
 * gated on) and answers `{entity}/newEntry` with a filled task — see `TaskPagesRest.newBaseDO`,
 * `newBaseDTO` and useNewEntryParams. Left out, the form opens with its required parent field empty and
 * the user picks one, which is what the tree's own "add" button does, as Wicket's does.
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
