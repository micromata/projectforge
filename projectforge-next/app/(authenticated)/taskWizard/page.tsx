"use client";

import { Suspense } from "react";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { useDocumentTitle } from "@/hooks/use-document-title";
import { TaskWizard } from "@/components/features/task/wizard/task-wizard";

/**
 * The structure wizard (`/next/taskWizard`), the migration of Wicket's `TaskWizardPage`.
 *
 * A route of its own, like `taskTree`: `taskWizard` is no REST category.
 *
 * The way here is the toolbar button of the task tree (see task-tree-action-bar.tsx).
 *
 * Admins only. Enforced by the endpoints behind it (`TaskWizardRest`) — this page and the entry that
 * leads here merely don't offer what would answer 403.
 */
export default function TaskWizardPage() {
  const t = useTranslations();
  const title = t("task.wizard.pageTitle");
  useDocumentTitle(title);

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">{title}</h1>
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        {/* The wizard reads `?highlightId=`, and `useSearchParams` needs this boundary under
            `output: "export"` — nothing is rendered during the prerender anyway. */}
        <Suspense>
          <TaskWizard />
        </Suspense>
      </div>
    </PageShell>
  );
}
