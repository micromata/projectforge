"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { useFormatContext } from "@/hooks/use-format";
import type { ForecastChartData } from "@/lib/rs/order";
import {
  moneyColumn,
  textColumn,
} from "../statistics/statistics-table-columns";
import { StatisticsTable } from "../statistics/statistics-table";
import { useStatisticsLabels } from "../statistics/use-statistics-labels";
import { formatChartMonth, spansMultipleYears } from "./order-forecast-series";

interface Row {
  month: string;
  label: string;
  ist: number;
  fixed: number;
  timeAndMaterials: number;
  weighted: number;
  total: number;
  planned: number;
}

/**
 * The cumulated forecast broken down per month: invoiced (IST) plus the remaining forecast split into
 * commissioned fixed price / flat rate, commissioned time and materials (extrapolated by the run rate) and
 * the not yet commissioned positions weighted by their probability. Shows how firm the forecast is. The
 * planned invoices are a "thereof" of the remaining forecast (their order positions are still part of it).
 */
export function ForecastBreakdownTable({ data }: { data: ForecastChartData }) {
  const t = useTranslations("fibu.auftrag.forecast.chart.breakdown");
  const labels = useStatisticsLabels();
  const ctx = useFormatContext();
  const showYear = spansMultipleYears(data);
  const rows = useMemo<Row[]>(
    () =>
      data.months.map((month, i) => ({
        month,
        label: formatChartMonth(month, ctx, showYear),
        ist: data.ist[i] ?? 0,
        fixed: data.forecastByKind?.COMMISSIONED_FIXED?.[i] ?? 0,
        timeAndMaterials:
          data.forecastByKind?.COMMISSIONED_TIME_AND_MATERIALS?.[i] ?? 0,
        weighted: data.forecastByKind?.WEIGHTED?.[i] ?? 0,
        total: data.total[i] ?? 0,
        planned: data.plannedInvoices?.[i] ?? 0,
      })),
    [data, ctx, showYear]
  );
  const columns = useMemo<ColumnDef<Row, unknown>[]>(
    () => [
      textColumn<Row>(
        "month",
        labels.month,
        (row) => row.label,
        120,
        labels.total
      ),
      moneyColumn<Row>("ist", t("ist"), (row) => row.ist, ctx),
      moneyColumn<Row>("fixed", t("fixed"), (row) => row.fixed, ctx, {
        size: 150,
      }),
      moneyColumn<Row>(
        "timeAndMaterials",
        t("timeAndMaterials"),
        (row) => row.timeAndMaterials,
        ctx,
        { size: 150 }
      ),
      moneyColumn<Row>("weighted", t("weighted"), (row) => row.weighted, ctx, {
        size: 150,
      }),
      moneyColumn<Row>("total", t("total"), (row) => row.total, ctx),
      moneyColumn<Row>("planned", t("planned"), (row) => row.planned, ctx, {
        size: 150,
      }),
    ],
    [t, labels, ctx]
  );
  return (
    <section className="space-y-2">
      <h3 className="text-sm font-semibold">{t("_")}</h3>
      <StatisticsTable<Row>
        columns={columns}
        data={rows}
        getRowId={(row) => row.month}
      />
    </section>
  );
}
