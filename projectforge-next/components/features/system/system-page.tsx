"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { useAuth } from "@/hooks/use-auth";
import { fetchSystemAdminData } from "@/lib/rs/system";
import { AlertMessageCard } from "./alert-message-card";
import { FormatLogEntriesCard } from "./format-log-entries-card";
import { ReindexCard } from "./reindex-card";
import { SystemMenuBar } from "./system-menu-bar";

/**
 * The System (administration) page ("/next/system"), successor of Wicket's `wa/admin`. A standalone,
 * action-driven admin page: a menu bar on top with the one-click actions (configuration incl. the
 * projectforge.properties editor, checks, database, Sipgate if configured, development), below it the cards that need input or
 * show progress: the site-wide alert message, reindexing the search indices and formatting log
 * entries. Admin-group only — every endpoint self-checks. The Wicket page is gone; old
 * `wa/admin` links are redirected here.
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
            <SystemMenuBar
              developmentMode={query.data.developmentMode}
              sipgateConfigured={query.data.sipgateConfigured}
            />
            <AlertMessageCard data={query.data} />
            <ReindexCard data={query.data} />
            <FormatLogEntriesCard />
          </>
        )}
      </div>
    </PageShell>
  );
}
