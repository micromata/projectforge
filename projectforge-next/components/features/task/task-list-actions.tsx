"use client";

import { TaskPerspectiveLink } from "@/components/shared/tasks/task-perspective-link";

/**
 * The one action of the task list: switch to the structure tree, as Wicket's `TaskListForm` offers it.
 *
 * Through `PageDef.listActions` — the declared slot left of the gear menu — so the toolbar itself needs
 * nothing task-specific. The filter the slot hands in is not read: the two perspectives keep their own
 * filters (the tree's is a `TaskFilter` in the session, see useTaskTree).
 */
export function TaskListActions() {
  return <TaskPerspectiveLink to="tree" />;
}
