"use client";

import { ListGearMenu } from "@/components/data-table";
import { AddEntryButton } from "@/components/shared/add-entry-button";
import { Separator } from "@/components/ui/separator";
import { TaskPerspectiveLink } from "./task-perspective-link";
import { TaskWizardLink } from "./task-wizard-link";
import { TASK_TREE_ROUTE, newTaskHref } from "./task-routes";

/**
 * The actions of the structure tree page: add a task, and the maintenance menu.
 *
 * A fragment, not a bar of its own: it fills the actions slot of the page's header row, the very slot
 * the list's toolbar fills with the same buttons (see PageTitleRow).
 *
 * The inventory is the content menu of Wicket's `TaskTreePage` — minus the favourites entry
 * (`UserPrefArea.TASK_FAVORITE`): this app manages them in the task select field itself (see
 * TaskFavoritesMenu), so they need no page of their own.
 *
 * Wicket's "list view" button *is* here, as the link to the other perspective on the same tasks (see
 * TaskPerspectiveLink), and so is the access wizard (see TaskWizardLink) — the list's toolbar carries
 * the mirror of both, in the same order and in the same place of the header row.
 *
 * The re-index entries come from the shared gear menu, so they behave as they do on every list.
 */
export function TaskTreeActionBar() {
  return (
    <>
      <TaskPerspectiveLink to="list" />
      <TaskWizardLink />
      <ListGearMenu entity="task" />
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
