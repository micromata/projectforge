"use client";

import { Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import {
  taskHref,
  SAVED_ID_PARAM,
  TASK_TREE_ROUTE,
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
      {/* Header and tree together read `?savedId=`, and `useSearchParams` needs this boundary under
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
 * A save on the task form returns here with the id of the written element (`?savedId=`, see
 * `EditDef.returnTargets` and useEditReturn), and that element is the row to mark — the server opens its
 * ancestors for it, so it is on screen even if its part of the tree was folded. Wicket does the same
 * with `PARAMETER_HIGHLIGHTED_ROW`.
 */
function TaskTreeBody() {
  const t = useTranslations();
  const router = useRouter();
  const savedId = Number(useSearchParams().get(SAVED_ID_PARAM));
  const highlightTaskId = savedId > 0 ? savedId : null;
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
