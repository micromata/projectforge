"use client";

import { ListGearMenu } from "@/components/data-table";
import { AddEntryButton } from "@/components/shared/add-entry-button";
import { Separator } from "@/components/ui/separator";
import { TaskPerspectiveLink } from "./task-perspective-link";
import { TaskWizardLink } from "./task-wizard-link";
import { TASK_TREE_ROUTE, newTaskHref } from "./task-routes";

export interface TaskTreeActionBarProps {
  /** Puts the tree's own filter back to its defaults, see `useTaskTree.resetFilter`. */
  onFilterReset: () => void;
}

/**
 * The actions of the structure tree page: add a task, and the maintenance menu.
 *
 * A fragment, not a bar of its own: it fills the actions slot of the page's header row, the very slot
 * the list's toolbar fills with the same buttons (see PageTitleRow).
 *
 * The inventory is the content menu of Wicket's `TaskTreePage` plus the reset button of its form —
 * minus the one entry this app cannot serve yet and therefore does not offer: the favourites
 * (`UserPrefListPage` for `UserPrefArea.TASK_FAVORITE`), still Wicket-only. The way to them is the
 * legacy link in the page's header (see projectforge-next/MIGRATION.md, step 3).
 *
 * Wicket's "list view" button *is* here, as the link to the other perspective on the same tasks (see
 * TaskPerspectiveLink), and so is the access wizard (see TaskWizardLink) — the list's toolbar carries
 * the mirror of both, in the same order and in the same place of the header row.
 *
 * The re-index entries come from the shared gear menu, so they behave as they do on every list; the
 * filter reset is passed in and declared as the page's `"own"`, because the tree's filter is a
 * `TaskFilter` in the session and not the entity's stored `MagicFilter` — asking the endpoint would
 * clear the *list* perspective's filter and column layout instead (see ListGearMenu.filterScope).
 */
export function TaskTreeActionBar({ onFilterReset }: TaskTreeActionBarProps) {
  return (
    <>
      <TaskPerspectiveLink to="list" />
      <TaskWizardLink />
      <ListGearMenu
        entity="task"
        onFilterReset={onFilterReset}
        filterScope="own"
      />
      {/* `!self-center`: with an explicit height the primitive's `self-stretch` degrades to
          flex-start (see ListToolbar, where the same separator stands). */}
      <Separator orientation="vertical" className="!h-5 !self-center" />
      {/* No parent: the form asks for one, which is what Wicket's `+` does too — its page passes no
          `PARAM_PARENT_TASK_ID` either. The per-row action adds below a specific task instead (see
          TaskTreeTable). */}
      <AddEntryButton href={newTaskHref({ returnTo: TASK_TREE_ROUTE })} />
    </>
  );
}
