"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { recallMarkedRowId } from "@/components/data-table";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import {
  taskHref,
  HIGHLIGHT_ID_PARAM,
  TASK_TREE_ROUTE,
  TASK_TREE_VIEW_SCOPE,
} from "@/components/shared/tasks/task-routes";
import { TaskTreeActionBar } from "@/components/shared/tasks/task-tree-action-bar";
import { TaskTreePanel } from "@/components/shared/tasks/task-tree-panel";
import { useTaskTree } from "@/components/shared/tasks/use-task-tree";

/**
 * The structure tree page (`/next/taskTree`), the migration of Wicket's `wa/taskTree`.
 *
 * A concrete route rather than a category of the generic list page: the tree is served by
 * `TaskServicesRest`, not by a list layout, and `taskTree` is no REST category — the entity behind it
 * is `task`. Hence the directory here, which shadows the `[category]` catch-all.
 *
 * Header and actions are the ones of the list perspective on the same tasks, down to the component
 * (see PageTitleRow and TaskTreeActionBar): the two pages differ in their table and in nothing else.
 * Which is why the page owns the tree's state instead of the panel — the gear menu of that header
 * resets the tree's filter.
 *
 * No `generateStaticParams`: there is no dynamic segment, so the static export emits this route
 * itself.
 */
export default function TaskTreePage() {
  return (
    <PageShell>
      {/* Header and tree together read `?highlightId=`, and `useSearchParams` needs this boundary under
          `output: "export"` — the first, empty read is the tree without a marked row. */}
      <Suspense>
        <TaskTreeBody />
      </Suspense>
    </PageShell>
  );
}

/**
 * Header and tree, split off for the `<Suspense>` above.
 *
 * The row to mark comes from one of two places, and both get the same treatment — the server opens its
 * ancestors (so it is on screen even where the tree was folded), the panel marks it and scrolls to it,
 * as Wicket's `PARAMETER_HIGHLIGHTED_ROW` does:
 * - a *save* returns with the written element's id in the url (`?highlightId=`, see
 *   `EditDef.returnTargets` and useEditReturn), which wins while it is there;
 * - *Cancel* and the browser's back button carry no id, so the last row opened from here stands in —
 *   remembered in the shared list memory the tree writes on select (see TaskTreeTable) and every list
 *   page uses through its `viewScope` (rememberMarkedRow/recallMarkedRowId).
 */
function TaskTreeBody() {
  const t = useTranslations();
  const router = useRouter();
  const urlHighlightId = Number(useSearchParams().get(HIGHLIGHT_ID_PARAM));
  const recalled = Number(recallMarkedRowId(TASK_TREE_VIEW_SCOPE));
  const highlightTaskId =
    urlHighlightId > 0 ? urlHighlightId : recalled > 0 ? recalled : null;
  const tree = useTaskTree({ highlightTaskId, showRootForAdmins: true });

  return (
    <>
      {/* No bottom border here: the tree's filter row belongs to the header just as the list's search
          row does, so the one line of the header sits below *it* (see TaskTreeTable). */}
      <div className="bg-background pb-2.5">
        <PageTitleRow
          category={t("menu.taskTree")}
          title={t("task.tree.perspective")}
          // Where the two entries of that menu this app cannot serve yet still are: the favourites
          // and, until it is migrated, anything else of Wicket's tree page.
          legacyUrl="wa/taskTree"
        >
          <TaskTreeActionBar onFilterReset={tree.resetFilter} />
        </PageTitleRow>
      </div>
      <div className="flex min-h-0 flex-1 flex-col gap-3 px-4 pb-4">
        <TaskTreePanel
          tree={tree}
          pageMode
          showRootForAdmins
          // Picking means "edit this task" here, and the root is a task with a page of its own. Every
          // other caller selects a task *for* something else, where the root is not a valid value.
          rootSelectable
          highlightTaskId={highlightTaskId}
          // `returnTo`, so cancel, save and the breadcrumb of the edit page lead back here rather than
          // to the task list — the tree is where the user came from (see useEditReturn).
          onSelect={(task) =>
            router.push(taskHref(task.id, { returnTo: TASK_TREE_ROUTE }))
          }
        />
      </div>
    </>
  );
}
