"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { PageShell } from "@/components/shared/page-shell";
import { PageTitleRow } from "@/components/shared/page-title-row";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { fetchPersonalStatistics } from "@/lib/rs/personal-statistics";
import { DisciplineChart } from "./discipline-chart";
import { DisciplineLegend } from "./discipline-legend";
import type { PersonalStatistics } from "./types";

/** The two chart series colours, the CSS tokens defined in globals.css (Soll/target red, Ist/actual green). */
const RED = "var(--chart-soll)";
const GREEN = "var(--chart-ist)";

/**
 * The personal statistics page ("My statistics", `/next/personalStatistics`), successor of Wicket's
 * `wa/personalStatistics`. A standalone, read-only page: for the logged-in user it shows two "timesheet
 * discipline" charts of the last N days — cumulative target vs. booked working hours, and how quickly
 * timesheets are booked against the target — each with the legend of its key figures.
 */
export function PersonalStatisticsPage() {
  const t = useTranslations();
  const data = useQuery({
    queryKey: ["personalStatistics"],
    queryFn: ({ signal }) => fetchPersonalStatistics(signal),
  });

  return (
    <PageShell>
      <PageTitleRow
        category={t("menu.personalStatistics")}
        title={t("personal.statistics.title")}
      />
      <div className="flex flex-col gap-8 px-4 pb-8 pt-2">
        {data.isPending && (
          <p className="text-sm text-muted-foreground">{t("loading")}</p>
        )}
        {data.isError && (
          <p className="text-sm text-destructive">
            {t("access.exception.noAccess")}
          </p>
        )}
        {data.data && <Charts stats={data.data} />}
      </div>
    </PageShell>
  );
}

/** The two charts with their legends, once the statistics have arrived. */
function Charts({ stats }: { stats: PersonalStatistics }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const { summary, lastNDays } = stats;
  return (
    <div className="flex flex-col gap-6">
      <p className="text-base font-medium">
        {t("personal.statistics.timesheetDisciplineChart.title")}
      </p>

      {/* The two charts side by side from md up (stacked on mobile), each kept small so both fit
          without scrolling — as the former Wicket page showed them next to each other. */}
      <div className="grid gap-8 md:grid-cols-2">
        <section className="flex flex-col gap-2">
          <DisciplineChart
            data={stats.workingHours}
            series={[
              {
                key: "soll",
                label: t("personal.statistics.chart.plannedHours"),
                color: RED,
              },
              {
                key: "ist",
                label: t("personal.statistics.chart.bookedHours"),
                color: GREEN,
              },
            ]}
            unitLabel={t("hours")}
            fractionDigits={0}
            ariaLabel={t("personal.statistics.chart.plannedHours")}
          />
          <DisciplineLegend
            messageKey="personal.statistics.timesheetDisciplineChart1.legend"
            values={{
              arg0: lastNDays,
              arg1: formatNumber(summary.planWorkingHours, ctx),
              arg2: formatNumber(summary.actualWorkingHours, ctx),
            }}
          />
        </section>

        <section className="flex flex-col gap-2">
          <DisciplineChart
            data={stats.bookingLatency}
            series={[
              {
                key: "actual",
                label: t("personal.statistics.chart.averageLatency"),
                color: RED,
              },
              {
                key: "plan",
                label: t("personal.statistics.chart.goal"),
                color: GREEN,
              },
            ]}
            unitLabel={t("days")}
            fractionDigits={1}
            ariaLabel={t("personal.statistics.chart.averageLatency")}
          />
          <DisciplineLegend
            messageKey="personal.statistics.timesheetDisciplineChart2.legend"
            values={{
              arg0: lastNDays,
              arg1: formatNumber(summary.plannedBookingLatency, ctx),
              arg2: formatNumber(summary.averageBookingLatency, ctx),
            }}
          />
        </section>
      </div>
    </div>
  );
}
