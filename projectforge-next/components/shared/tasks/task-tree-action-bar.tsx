"use client";

import { ListGearMenu } from "@/components/data-table";
import { AddEntryButton } from "@/components/shared/add-entry-button";
import { Separator } from "@/components/ui/separator";
import { TaskPerspectiveLink } from "./task-perspective-link";
import { TASK_TREE_ROUTE, newTaskHref } from "./task-routes";

export interface TaskTreeActionBarProps {
  /** Puts the tree's own filter back to its defaults, see `useTaskTree.resetFilter`. */
  onFilterReset: () => void;
}

/**
 * The actions of the structure tree page: add a task, and the maintenance menu.
 *
 * The inventory is the content menu of Wicket's `TaskTreePage` plus the reset button of its form —
 * minus two entries this app cannot serve yet and therefore does not offer: the favourites
 * (`UserPrefListPage` for `UserPrefArea.TASK_FAVORITE`) and the task wizard (`TaskWizardPage`), both
 * still Wicket-only. The way to them is the legacy link in the page's header (see
 * projectforge-next/MIGRATION.md, step 3).
 *
 * Wicket's "list view" button *is* here, as the link to the other perspective on the same tasks (see
 * TaskPerspectiveLink); the list has the mirror of it in its toolbar.
 *
 * The re-index entries come from the shared gear menu, so they behave as they do on every list; the
 * filter reset is passed in, because the tree's filter is a `TaskFilter` in the session and not the
 * entity's stored `MagicFilter` (see ListGearMenu.onFilterReset).
 */
export function TaskTreeActionBar({ onFilterReset }: TaskTreeActionBarProps) {
  return (
    <div className="flex items-center justify-end gap-3">
      <TaskPerspectiveLink to="list" />
      <ListGearMenu entity="task" onFilterReset={onFilterReset} />
      {/* `!self-center`: with an explicit height the primitive's `self-stretch` degrades to
          flex-start (see ListToolbar, where the same separator stands). */}
      <Separator orientation="vertical" className="!h-5 !self-center" />
      {/* No parent: a task added from here hangs below the root, which is what Wicket's `+` does. The
          per-row action adds below a specific task instead (see TaskTreeTable). */}
      <AddEntryButton href={newTaskHref({ returnTo: TASK_TREE_ROUTE })} />
    </div>
  );
}
