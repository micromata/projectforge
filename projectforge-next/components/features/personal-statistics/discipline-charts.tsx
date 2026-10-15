"use client";

import { useTranslations } from "next-intl";
import { ChartDashboard } from "@/components/shared/dashboard/chart-dashboard";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { CHART_ROLE } from "@/lib/charts/roles";
import { DisciplineChart } from "./discipline-chart";
import { DisciplineLegend } from "./discipline-legend";
import type { PersonalStatistics } from "./types";

/** The two chart series colours from the shared role palette (Soll/target red, Ist/actual green). */
const RED = CHART_ROLE.target;
const GREEN = CHART_ROLE.actual;

/** The two charts with their legends, once the statistics have arrived. */
export function DisciplineCharts({ stats }: { stats: PersonalStatistics }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const { summary, lastNDays } = stats;
  return (
    <div className="flex flex-col gap-6">
      <p className="text-base font-medium">
        {t("personal.statistics.timesheetDisciplineChart.title")}
      </p>

      {/* The two charts side by side from lg up by default (stacked on mobile), each kept small so both
          fit without scrolling — as the former Wicket page showed them next to each other. */}
      <ChartDashboard
        id="personalStatistics.discipline"
        tiles={[
          {
            id: "workingHours",
            title: t("personal.statistics.chart.workingHours"),
            defaultWidth: "half",
            defaultHeight: "S",
            render: (className) => (
              <div className="flex flex-col gap-2">
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
                  ariaLabel={t("personal.statistics.chart.workingHours")}
                  className={className}
                />
                <DisciplineLegend
                  messageKey="personal.statistics.timesheetDisciplineChart1.legend"
                  values={{
                    arg0: lastNDays,
                    arg1: formatNumber(summary.planWorkingHours, ctx),
                    arg2: formatNumber(summary.actualWorkingHours, ctx),
                  }}
                />
              </div>
            ),
          },
          {
            id: "bookingLatency",
            title: t("personal.statistics.chart.bookingLatency"),
            defaultWidth: "half",
            defaultHeight: "S",
            render: (className) => (
              <div className="flex flex-col gap-2">
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
                  ariaLabel={t("personal.statistics.chart.bookingLatency")}
                  className={className}
                />
                <DisciplineLegend
                  messageKey="personal.statistics.timesheetDisciplineChart2.legend"
                  values={{
                    arg0: lastNDays,
                    arg1: formatNumber(summary.plannedBookingLatency, ctx),
                    arg2: formatNumber(summary.averageBookingLatency, ctx),
                  }}
                />
              </div>
            ),
          },
        ]}
      />
    </div>
  );
}
