"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import { TaskTreePanel } from "@/components/shared/tasks/task-tree-panel";
import { useInitialList } from "@/hooks/use-initial-list";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";

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
  // A task is edited on its legacy page — that one isn't migrated. The template (`:id` for the id)
  // comes from the backend, because whether the id is a path segment or a query parameter depends on
  // which app serves the page. `task` is the entity of TaskPagesRest; `taskTree` is no category.
  const editUrlTemplate = useInitialList("task").data?.legacyEditPage;

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
          onSelect={(task) => {
            if (!editUrlTemplate) return;
            const url = editUrlTemplate.replace(":id", String(task.id));
            window.location.href = toAbsoluteUrl(resolveMenuUrl(url));
          }}
        />
      </div>
    </PageShell>
  );
}
