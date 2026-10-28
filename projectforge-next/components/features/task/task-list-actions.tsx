"use client";

import { TaskPerspectiveLink } from "@/components/shared/tasks/task-perspective-link";
import { ProjectWizardLink } from "@/components/shared/tasks/project-wizard-link";
import { TaskWizardLink } from "@/components/shared/tasks/task-wizard-link";

/**
 * The actions of the task list: switch to the structure tree, as Wicket's `TaskListForm` offers it,
 * the access wizard and the project wizard — the same buttons the tree page has, in the same order, so the two
 * perspectives differ in their table and in nothing else (see TaskTreeActionBar).
 *
 * Through `PageDef.listActions` — the declared slot left of the gear menu — so the toolbar itself needs
 * nothing task-specific. The filter the slot hands in is not read: the two perspectives keep their own
 * filters (the tree's is a `TaskFilter` in the session, see useTaskTree).
 */
export function TaskListActions() {
  return (
    <>
      <TaskPerspectiveLink to="tree" />
      <TaskWizardLink />
      <ProjectWizardLink />
    </>
  );
}
