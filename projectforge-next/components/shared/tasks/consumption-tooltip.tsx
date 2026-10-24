import type { TaskConsumption } from "@/lib/rs/task";

/**
 * The consumption bar's tooltip as a calculation — budget minus booked, the remaining days as its result
 * line — so nobody has to subtract in their head:
 *
 * ```
 * Budget        18,00 PT
 * Gebucht    −   8,78 PT   49 %
 * ──────────────────────────
 * Rest           9,22 PT
 * 03.08.2026–12.10.2026
 * ```
 *
 * Overbooked, the result reads "Überbucht" in the overbooked colour; without a budget only the booked
 * days remain. The figures come formatted from the backend (`rest/task/Consumption.kt`); a value without
 * them falls back to its one-line `title`.
 *
 * No hooks, and `t` passed in: it is also rendered from a table cell renderer (see ConsumptionCell).
 */
export function ConsumptionTooltip({
  consumption,
  t,
}: {
  consumption: TaskConsumption;
  t: (key: string) => string;
}) {
  const { booked, budget, remaining, overbooked, percentage, period, title } =
    consumption;
  if (!booked) return <span className="whitespace-pre-wrap">{title}</span>;
  return (
    <span className="flex flex-col gap-1">
      <span className="font-semibold">{t("task.consumption")}</span>
      <span className="grid grid-cols-[auto_auto_auto_auto] gap-x-2 tabular-nums">
        {budget && (
          <>
            <span>{t("task.consumptionTooltip.budget")}</span>
            <span />
            <span className="text-right">{budget}</span>
            <span />
          </>
        )}
        <span>{t("task.consumptionTooltip.booked")}</span>
        <span className="text-right">{budget ? "−" : ""}</span>
        <span className="text-right">{booked}</span>
        <span className="text-right">
          {budget && percentage != null ? `${percentage} %` : ""}
        </span>
        {budget && remaining && (
          <>
            <span className="col-span-4 my-0.5 border-t border-current opacity-60" />
            <span
              className={
                overbooked
                  ? "font-semibold text-[var(--consumption-over)]"
                  : "font-semibold"
              }
            >
              {t(
                overbooked
                  ? "task.consumptionTooltip.overbooked"
                  : "task.consumptionTooltip.remaining"
              )}
            </span>
            <span />
            <span
              className={
                overbooked
                  ? "text-right font-semibold text-[var(--consumption-over)]"
                  : "text-right font-semibold"
              }
            >
              {remaining}
            </span>
            <span />
          </>
        )}
      </span>
      {period && <span className="opacity-70">{period}</span>}
    </span>
  );
}
