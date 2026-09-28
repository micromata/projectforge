"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { Card, CardContent } from "@/components/ui/card";
import { useAuth } from "@/hooks/use-auth";
import { fetchPluginList } from "@/lib/rs/plugins";
import { PluginRow } from "./plugin-row";

/**
 * The Plugins (administration) page ("/next/plugins"), successor of Wicket's `PluginListPage`. A
 * standalone, admin-only action page: it lists every available plugin with an activate/deactivate
 * button; a toggle only takes effect after a restart. Plugins forced active via
 * `projectforge.plugins.ensure-active` cannot be deactivated here. Admin-group only — every endpoint
 * self-checks. The classic Wicket page has been removed; this is the only Plugins page now.
 */
export function PluginListPage() {
  const t = useTranslations();
  const { isAdmin, isLoading } = useAuth();

  const query = useQuery({
    queryKey: ["plugins"],
    queryFn: ({ signal }) => fetchPluginList(signal),
    enabled: isAdmin,
  });

  const ensureActiveIds = query.data?.ensureActivePluginIds ?? [];

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.pluginAdmin")}
        title={t("system.pluginAdmin.title")}
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
            <p className="text-sm font-medium text-muted-foreground">
              {t("system.pluginAdmin.restartRequired")}
            </p>
            {ensureActiveIds.length > 0 && (
              <p className="text-sm text-muted-foreground">
                {t("system.pluginAdmin.ensureActive", {
                  arg0: ensureActiveIds.join(", "),
                })}
              </p>
            )}
            <Card>
              <CardContent className="flex flex-col divide-y p-0">
                {query.data.plugins.map((plugin) => (
                  <PluginRow key={plugin.id} plugin={plugin} />
                ))}
              </CardContent>
            </Card>
          </>
        )}
      </div>
    </PageShell>
  );
}
