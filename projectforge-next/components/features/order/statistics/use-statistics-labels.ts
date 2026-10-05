"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";

/**
 * The column and tab labels of the data tables of the order statistics, in one place. Most are the
 * bundle's shared keys (customer, project, cost units, the order and invoice fields, the series of the
 * charts); only what has no key elsewhere lives under `fibu.auftrag.statistics.tables`.
 */
export function useStatisticsLabels() {
  const t = useTranslations();
  return useMemo(
    () => ({
      // Shared keys:
      customer: t("fibu.kunde._"),
      project: t("fibu.projekt._"),
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
      prevYear: t("fibu.auftrag.forecast.chart.prevYear"),
      prevPrevYear: t("fibu.auftrag.forecast.chart.prevPrevYear"),
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
      invoicesPrevYear: t("fibu.auftrag.statistics.tables.invoicesPrevYear"),
      invoicesPrevPrevYear: t(
        "fibu.auftrag.statistics.tables.invoicesPrevPrevYear"
      ),
      orderStatus: t("fibu.auftrag.statistics.tables.orderStatus"),
      positionStatus: t("fibu.auftrag.statistics.tables.positionStatus"),
      toBeInvoicedSum: t("fibu.auftrag.statistics.tables.toBeInvoicedSum"),
      bookedDate: t("fibu.auftrag.statistics.tables.bookedDate"),
      warning: t("fibu.auftrag.statistics.tables.warning"),
      preliminaryLegend: t("fibu.auftrag.statistics.tables.preliminaryLegend"),
    }),
    [t]
  );
}

export type StatisticsLabels = ReturnType<typeof useStatisticsLabels>;
