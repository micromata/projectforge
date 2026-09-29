import { seriesKey } from "@/lib/charts/series";
import { formatMonthName, type FormatContext } from "@/lib/format";
import type { InvoiceNetSumChartData } from "@/lib/rs/invoice";

/**
 * The invoice-specific shaping of the net-sum chart data (see `invoice-charts-view.tsx`): the two charts of
 * the "Grafiken" tab — the monthly bars and the cumulative curves — are the same four years, so both read
 * from one set of series keys and the shared categorical palette/config in `lib/charts/series.ts`. Only the
 * numbers differ (a month's sum for the bars, its running total for the curves), which is what
 * {@link monthlyRows} and {@link cumulativeRows} produce. The month-axis label helpers live here too.
 */

/** The keys of every series present, in order — what both charts map over to place their bars and lines. */
export function seriesKeys(data: InvoiceNetSumChartData): string[] {
  return data.series.map((_, index) => seriesKey(index));
}

/** A row per month, each year's net sum of that month under its series key — the bars' data. */
export function monthlyRows(
  data: InvoiceNetSumChartData
): Record<string, number | string>[] {
  return data.months.map((month, monthIndex) => {
    const row: Record<string, number | string> = { month };
    data.series.forEach((serie, index) => {
      row[seriesKey(index)] = serie.monthly[monthIndex] ?? 0;
    });
    return row;
  });
}

/** The same rows with each year's running total instead of its monthly sum — the cumulative curves' data. */
export function cumulativeRows(
  data: InvoiceNetSumChartData
): Record<string, number | string>[] {
  const totals = data.series.map(() => 0);
  return data.months.map((month, monthIndex) => {
    const row: Record<string, number | string> = { month };
    data.series.forEach((serie, index) => {
      totals[index] += serie.monthly[monthIndex] ?? 0;
      row[seriesKey(index)] = totals[index];
    });
    return row;
  });
}

/**
 * A month of the x-axis ("yyyy-MM") as its name in the user's language. The two-digit year is appended only
 * when the period crosses a year boundary, so a plain calendar year reads "Januar … Dezember" without noise.
 */
export function formatChartMonth(
  isoMonth: string,
  ctx: FormatContext,
  showYear: boolean
): string {
  const [year, month] = isoMonth.split("-");
  const name = formatMonthName(Number(month), ctx);
  return showYear ? `${name} '${year.slice(2)}` : name;
}

/** Whether the reference period spans more than one calendar year — then the month labels carry the year. */
export function spansMultipleYears(data: InvoiceNetSumChartData): boolean {
  const years = new Set(data.months.map((month) => month.slice(0, 4)));
  return years.size > 1;
}
