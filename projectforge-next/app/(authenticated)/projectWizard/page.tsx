"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { useDocumentTitle } from "@/hooks/use-document-title";
import { ProjectWizard } from "@/components/features/project/wizard/project-wizard";

/**
 * The project wizard (`/next/projectWizard`): customer, project, structure element, groups and access
 * rights in one go, shown as the customer wizard ("Kundenassistent"). New in this app, there is no
 * Wicket predecessor.
 *
 * A concrete route like `taskWizard`: `projectWizard` is no REST category, and the directory here
 * shadows the `[category]` catch-all.
 *
 * For the users who may create customers, projects and structure elements. Enforced by the endpoints
 * behind it (`ProjectWizardRest`) — this page and the entries that lead here merely don't offer what
 * would answer 403.
 */
export default function ProjectWizardPage() {
  const t = useTranslations();
  const title = t("project.wizard.pageTitle");
  useDocumentTitle(title);

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">{title}</h1>
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        <ProjectWizard />
      </div>
    </PageShell>
  );
}
