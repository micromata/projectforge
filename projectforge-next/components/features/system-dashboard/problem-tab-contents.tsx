"use client";

import { useTranslations } from "next-intl";
import { TabsContent } from "@/components/ui/tabs";
import {
  type SearchParamChanges,
  updateSearchParams,
} from "@/lib/search-params";
import { AdminErrorsOverview } from "./admin-errors-overview";
import { AdminErrorsProblems } from "./admin-errors-problems";
import {
  SCOPE_PARAM,
  SUBSYSTEM_PARAM,
  useProblemDashboard,
} from "./use-problem-dashboard";

/** `SchedulerSubsystemStatusProvider.id`. */
const SCHEDULER_SUBSYSTEM = "scheduler";

/** The overview and the problems tab of the system dashboard, admin group only. */
export function ProblemTabContents({
  problems,
  setTab,
}: {
  problems: ReturnType<typeof useProblemDashboard>;
  setTab: (tab: string, with_?: SearchParamChanges) => void;
}) {
  const t = useTranslations();
  const { list, subsystems, filter, setFilter, search, setSearch } = problems;
  const data = list.data;
  if (!data) {
    // Until the problems are there, both tabs show the same state.
    const state = (
      <div className="pb-8 pt-2 text-sm">
        {problems.denied ? (
          <p className="text-destructive">{t("access.exception.noAccess")}</p>
        ) : list.isError ? (
          <p className="text-destructive">{t("errorpage.title")}</p>
        ) : (
          <p className="text-muted-foreground">{t("loading")}</p>
        )}
      </div>
    );
    return (
      <>
        <TabsContent value="overview">{state}</TabsContent>
        <TabsContent value="problems">{state}</TabsContent>
      </>
    );
  }
  const disabled = !data.enabled && (
    <p className="text-sm text-destructive">
      {t("system.admin.adminErrors.disabled")}
    </p>
  );

  return (
    <>
      <TabsContent value="overview" className="text-sm">
        {disabled}
        <AdminErrorsOverview
          summary={data.summary}
          subsystems={subsystems.data}
          subsystemsError={subsystems.isError}
          onOpenSummary={(it) => {
            // The key figures count all problems: a subsystem chosen before is dropped.
            setFilter({
              status: it.status,
              category: it.category,
              days: it.days,
            });
            setSearch("");
            setTab("problems", {
              [SUBSYSTEM_PARAM]: null,
              [SCOPE_PARAM]: it.scope ?? null,
            });
          }}
          onOpenSubsystem={(it) =>
            // The scheduler's tile shows its jobs rather than its problems: a failed job is no log problem.
            it.id === SCHEDULER_SUBSYSTEM
              ? setTab("scheduler")
              : setTab("problems", {
                  [SUBSYSTEM_PARAM]: it.id,
                  [SCOPE_PARAM]: null,
                })
          }
        />
      </TabsContent>
      <TabsContent
        value="problems"
        className="flex min-h-0 flex-col gap-3 text-sm"
      >
        {disabled}
        <AdminErrorsProblems
          data={data}
          isFetching={list.isFetching}
          filter={filter}
          subsystemTitle={problems.subsystemTitle}
          onFilterChange={setFilter}
          onRemoveSubsystem={() =>
            updateSearchParams({ [SUBSYSTEM_PARAM]: null }, "push")
          }
          onRemoveScope={() =>
            updateSearchParams({ [SCOPE_PARAM]: null }, "push")
          }
          search={search}
          onSearchChange={setSearch}
          onOpen={(entry) => problems.setDetailId(entry.id)}
        />
      </TabsContent>
    </>
  );
}
