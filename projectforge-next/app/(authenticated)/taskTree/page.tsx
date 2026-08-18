"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import { TaskTreePanel } from "@/components/shared/tasks/task-tree-panel";
import { TASK_TREE_ROUTE } from "@/components/features/task/task.page";

/**
 * The structure tree page (`/next/taskTree`), the migration of Wicket's `wa/taskTree`.
 *
 * A concrete route rather than a category of the generic list page: the tree is served by
 * `TaskServicesRest`, not by a list layout, and `taskTree` is no REST category — the entity behind it
 * is `task`. Hence the directory here, which shadows the `[category]` catch-all.
 *
 * No `generateStaticParams`: there is no dynamic segment, so the static export emits this route
 * itself.
 */
export default function TaskTreePage() {
  const t = useTranslations();
  const router = useRouter();

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">
          {t("menu.taskTree")}
        </h1>
        <div className="flex-1" />
        <LegacyPageLink url="wa/taskTree" />
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        <TaskTreePanel
          showRootForAdmins
          // Picking means "edit this task" here, and the root is a task with a page of its own. Every
          // other caller selects a task *for* something else, where the root is not a valid value.
          rootSelectable
          // `returnTo`, so cancel, save and the breadcrumb of the edit page lead back here rather than
          // to the task list — the tree is where the user came from (see useEditReturn).
          onSelect={(task) =>
            router.push(
              `/task/${task.id}?returnTo=${encodeURIComponent(TASK_TREE_ROUTE)}`
            )
          }
        />
      </div>
    </PageShell>
  );
}
