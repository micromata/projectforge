"use client";

import { Suspense } from "react";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { TaskWizard } from "@/components/features/task/wizard/task-wizard";

/**
 * The structure wizard (`/next/taskWizard`), the migration of Wicket's `TaskWizardPage`.
 *
 * A concrete route rather than a category of the generic list page, like `taskTree`: `taskWizard` is no
 * REST category, and the directory here shadows the `[category]` catch-all.
 *
 * No `LegacyPageLink` in the header: Wicket's wizard is not a bookmarkable page but one pushed from
 * `TaskTreePage`, so there is no legacy url to offer. The way here is that page's own toolbar button
 * (see task-tree-action-bar.tsx).
 *
 * Admins only. Enforced by the endpoints behind it (`TaskWizardRest`) — this page and the entry that
 * leads here merely don't offer what would answer 403.
 */
export default function TaskWizardPage() {
  const t = useTranslations();

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">
          {t("task.wizard.pageTitle")}
        </h1>
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
