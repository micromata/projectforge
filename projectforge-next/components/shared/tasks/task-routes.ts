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
 * The element the wizard opens on, as a parameter of its url — how the task's own form starts the
 * wizard for the element on screen (see TASK_PAGE.crossLinks).
 *
 * Not [HIGHLIGHT_ID_PARAM], although both end up in the same first step: that one means "this element
 * was just created for the wizard" and comes with the groups picked before that detour
 * (see wizard-handover.ts). This one is a plain preset, and the wizard is otherwise fresh.
 */
export const WIZARD_TASK_PARAM = "taskId";

/**
 * The url of the structure wizard, opened on `taskId` where one is given.
 *
 * Nullable, because a caller passes the id of an entity as it comes from the backend
 * (`TaskDetail.id`) — without one the wizard opens as it does from the tree, on an empty first step.
 */
export function taskWizardHref(options?: { taskId?: number | null }): string {
  return `${TASK_WIZARD_ROUTE}${query({
    [WIZARD_TASK_PARAM]: options?.taskId ?? undefined,
  })}`;
}

/**
 * The parameter a caller of the task form gets the id of the written element back in — set by the form
 * on a successful save (`EditDef.returnTargets`, see useEditReturn) and read by the page that returns.
 *
 * Named for what the returning page does with it rather than where it came from: two callers ask for
 * it, and the tree *highlights* the row (Wicket's `PARAMETER_HIGHLIGHTED_ROW`) while the wizard goes on
 * with the element that was just created — the same id, put to two uses. Beside the routes for the
 * same reason they are here — the form's declaration and both pages need it, and none of them may
 * import the other.
 */
export const HIGHLIGHT_ID_PARAM = "highlightId";

/**
 * What the tree page remembers the last opened row under, so Cancel and the browser's back button mark
 * it again the way a save's [HIGHLIGHT_ID_PARAM] does (see the shared list memory in use-list-view-memory
 * — rememberMarkedRow/recallMarkedRowId, which every list page uses through its `viewScope`).
 *
 * Its own scope, distinct from the task *list*'s (`"task"`, its entity name): the two perspectives
 * onto the same tasks should not mark each other's rows. Only the tree *page* writes it — the select
 * dialog picks a value and never returns to a highlight.
 */
export const TASK_TREE_VIEW_SCOPE = "taskTree";

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
