"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { useAuth } from "@/hooks/use-auth";
import { fetchSystemAdminData } from "@/lib/rs/system";
import { AlertMessageCard } from "./alert-message-card";
import { FormatLogEntriesCard } from "./format-log-entries-card";
import { JcrCard } from "./jcr-card";
import { ReindexCard } from "./reindex-card";
import { SystemActionGroups } from "./system-action-groups";

/**
 * The System (administration) page ("/next/system"), successor of Wicket's `wa/admin`. A standalone,
 * action-driven admin page: set the site-wide alert message, reindex the search indices (with an
 * inline progress bar), format log entries, prepare the JCR replacement, and run the
 * caches/configuration/checks/database/dev actions. Admin-group only — every endpoint self-checks. The Wicket page is gone; old `wa/admin`
 * links are redirected here.
 */
export function SystemPage() {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();

  const query = useQuery({
    queryKey: ["system"],
    queryFn: ({ signal }) => fetchSystemAdminData(signal),
    enabled: isAdmin,
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.system")}
        title={t("system.admin.title")}
      />
      <div className="flex flex-col gap-4 px-4 pb-8 pt-2">
        {!isLoading && !isAdmin && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {query.isPending && isAdmin && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {query.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {query.data && (
          <>
            <AlertMessageCard data={query.data} />
            <ReindexCard data={query.data} />
            <SystemActionGroups data={query.data} />
            <JcrCard data={query.data} />
            <FormatLogEntriesCard />
          </>
        )}
      </div>
    </PageShell>
  );
}
