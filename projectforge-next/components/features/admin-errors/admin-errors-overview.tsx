"use client";

import { useTranslations } from "next-intl";
import type { LogGroupSummary, SubsystemEntry } from "@/lib/rs/admin-errors";
import { AdminErrorsSummary } from "./admin-errors-summary";
import { AdminSubsystemTiles } from "./admin-subsystem-tiles";

/**
 * The overview tab of the problem dashboard: the key figures of all problems and a tile per active subsystem or
 * interface. A tile click shows its problems ([onOpenSubsystem]).
 */
export function AdminErrorsOverview({
  summary,
  subsystems,
  subsystemsError,
  onOpenSubsystem,
}: {
  summary: LogGroupSummary;
  subsystems: SubsystemEntry[] | undefined;
  subsystemsError: boolean;
  onOpenSubsystem: (subsystem: SubsystemEntry) => void;
}) {
  const t = useTranslations();
  return (
    <div className="space-y-4">
      <AdminErrorsSummary summary={summary} />
      <section className="space-y-2">
        <h2 className="text-sm font-semibold">
          {t("system.admin.adminErrors.subsystems._")}
        </h2>
        {subsystems ? (
          <AdminSubsystemTiles
            subsystems={subsystems}
            onOpen={onOpenSubsystem}
          />
        ) : subsystemsError ? (
          <p className="text-sm text-destructive">{t("errorpage.title")}</p>
        ) : (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
      </section>
    </div>
  );
}
