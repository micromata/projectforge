"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import { previousPeriodLabels } from "@/lib/period-label";

/**
 * The column and tab labels of the data tables of the order statistics, in one place. Most are the
 * bundle's shared keys (customer, project, cost units, the order and invoice fields, the series of the
 * charts); only what has no key elsewhere lives under `fibu.auftrag.statistics.tables`.
 *
 * [firstMonth] (`yyyy-MM`) is the first month of the 12 months window of the tables: the labels of the two
 * comparison periods name their years (`2024/2025`) instead of "previous year".
 */
export function useStatisticsLabels(firstMonth?: string | null) {
  const t = useTranslations();
  return useMemo(() => {
    const period = previousPeriodLabels(firstMonth);
    return {
      // Shared keys:
      customer: t("fibu.kunde._"),
      project: t("fibu.projekt._"),
      projectGroup: t("fibu.projectGroup"),
      kost: t("fibu.auftrag.contributionMargin.kost"),
      kost2: t("fibu.kost2._"),
      order: t("fibu.auftrag._"),
      invoice: t("fibu.rechnung._"),
      date: t("fibu.rechnung.datum._"),
      month: t("calendar.month._"),
      hours: t("hours"),
      status: t("status"),
      title: t("title"),
      subject: t("fibu.rechnung.betreff"),
      positionText: t("fibu.rechnung.text"),
      art: t("fibu.auftrag.position.art._"),
      paymentType: t("fibu.auftrag.position.paymenttype._"),
      personDays: t("projectmanagement.personDays.short"),
      netSum: t("fibu.auftrag.nettoSumme._"),
      net: t("fibu.common.netto"),
      weightedNetSum: t("fibu.auftrag.nettoSumme.weighted"),
      probability: t("fibu.auftrag.probabilityOfOccurrence.weighted._"),
      invoicedSum: t("fibu.title.fakturiert"),
      periodOfPerformance: t("fibu.periodOfPerformance._"),
      forecastType: t("fibu.auftrag.forecastType._"),
      remaining: t("rest"),
      difference: t("fibu.common.difference"),
      forecast: t("fibu.auftrag.forecast.chart.total"),
      plan: t("fibu.auftrag.forecast.chart.plan"),
      prevYear: t("fibu.auftrag.forecast.chart.prevYear", {
        arg0: period.prevYear,
      }),
      prevPrevYear: t("fibu.auftrag.forecast.chart.prevPrevYear", {
        arg0: period.prevPrevYear,
      }),
      revenue: t("fibu.auftrag.contributionMargin.revenue"),
      costs: t("fibu.auftrag.contributionMargin.costs"),
      profit: t("fibu.auftrag.contributionMargin.profit"),
      percentage: t("fibu.auftrag.contributionMargin.percentage"),
      total: t("fibu.auftrag.contributionMargin.total"),
      projects: t("fibu.projekt.projekte"),
      positions: t("fibu.auftrag.positions"),
      invoices: t("fibu.rechnung.rechnungen"),
      timesheets: t("timesheet.timesheets"),
      // Only here:
      heading: t("fibu.auftrag.statistics.tables.heading"),
      months: t("fibu.auftrag.statistics.tables.months"),
      invoicesPrevYear: t("fibu.auftrag.statistics.tables.invoicesPrevYear", {
        arg0: period.prevYear,
      }),
      invoicesPrevPrevYear: t(
        "fibu.auftrag.statistics.tables.invoicesPrevPrevYear",
        {
          arg0: period.prevPrevYear,
        }
      ),
      orderStatus: t("fibu.auftrag.statistics.tables.orderStatus"),
      positionStatus: t("fibu.auftrag.statistics.tables.positionStatus"),
      toBeInvoicedSum: t("fibu.auftrag.statistics.tables.toBeInvoicedSum"),
      bookedDate: t("fibu.auftrag.statistics.tables.bookedDate"),
      warning: t("fibu.auftrag.statistics.tables.warning"),
      forecastDetails: t("fibu.auftrag.statistics.tables.forecastDetails"),
      preliminaryLegend: t("fibu.auftrag.statistics.tables.preliminaryLegend"),
    };
  }, [t, firstMonth]);
}

export type StatisticsLabels = ReturnType<typeof useStatisticsLabels>;
