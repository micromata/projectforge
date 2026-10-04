"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { useDocumentTitle } from "@/hooks/use-document-title";
import { TeamCalImport } from "@/components/features/teamcal-import/teamcal-import";

/**
 * The ICS import of team events (`/next/teamCalImport`), reached from the calendar's more menu and the
 * calendar administration (`react/teamCal`). `?teamCalId=` preselects the target calendar.
 */
export default function TeamCalImportPage() {
  const t = useTranslations();
  const title = t("plugins.teamcal.import.ics.title");
  useDocumentTitle(title);

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">{title}</h1>
      </div>
      <div className="flex min-h-0 flex-1 flex-col p-4">
        {/* `useSearchParams` needs this boundary under `output: "export"`. */}
        <Suspense>
          <TeamCalImportWithParams />
        </Suspense>
      </div>
    </PageShell>
  );
}

function TeamCalImportWithParams() {
  const raw = useSearchParams().get("teamCalId");
  const id = raw != null ? Number(raw) : NaN;
  return <TeamCalImport initialTeamCalId={Number.isInteger(id) ? id : null} />;
}
