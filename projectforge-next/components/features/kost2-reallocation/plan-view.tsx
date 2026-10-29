"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { column } from "./column";
import { FormAlert } from "@/components/shared/form-alert";
import { useFormatContext } from "@/hooks/use-format";
import { formatCurrency, type FormatContext } from "@/lib/format";
import type {
  Kost2ReallocationAmount,
  Kost2ReallocationMove,
  Kost2ReallocationPlan,
  Kost2ReallocationRecord,
  Kost2ReallocationSum,
} from "@/lib/rs/kost2-reallocation";

function formatAmounts(list: Kost2ReallocationAmount[], format: FormatContext) {
  return list
    .map((a) => `${a.kost2}: ${formatCurrency(a.amount, format)}`)
    .join(" | ");
}

/**
 * What a run changes (the preview, or the result of applying it): errors (refusing the apply), warnings, sums,
 * records and time sheets.
 */
export function PlanView({ plan }: { plan: Kost2ReallocationPlan }) {
  const t = useTranslations();
  const format = useFormatContext();
  const sums = useMemo<ColumnDef<Kost2ReallocationSum, unknown>[]>(
    () => [
      column("kost2", t("fibu.kost2._"), (r) => r.kost2, 140),
      column(
        "before",
        t("fibu.kost2Reallocation.before"),
        (r) => formatCurrency(r.before, format),
        140,
        "right"
      ),
      column(
        "after",
        t("fibu.kost2Reallocation.after"),
        (r) => formatCurrency(r.after, format),
        140,
        "right"
      ),
    ],
    [t, format]
  );
  const records = useMemo<ColumnDef<Kost2ReallocationRecord, unknown>[]>(
    () => [
      column("satznr", t("fibu.buchungssatz.satznr"), (r) => r.satznr, 130),
      column("konto", t("fibu.buchungssatz.konto"), (r) => r.konto, 80),
      column("text", t("fibu.buchungssatz.text"), (r) => r.text, 240),
      column(
        "old",
        t("fibu.kost2Reallocation.before"),
        (r) => formatAmounts(r.oldAmounts, format),
        320
      ),
      column(
        "new",
        t("fibu.kost2Reallocation.after"),
        (r) => formatAmounts(r.newAmounts, format),
        320
      ),
    ],
    [t, format]
  );
  const moves = useMemo<ColumnDef<Kost2ReallocationMove, unknown>[]>(
    () => [
      column("user", t("timesheet.user"), (r) => r.user, 160),
      column(
        "month",
        t("calendar.month._"),
        (r) => `${r.year}-${String(r.month).padStart(2, "0")}`,
        90
      ),
      column("hours", t("hours"), (r) => r.hours.toFixed(2), 70),
      column(
        "task",
        t("task._"),
        (r) =>
          r.oldTask === r.newTask ? r.oldTask : `${r.oldTask} → ${r.newTask}`,
        360
      ),
      column(
        "kost2",
        t("fibu.kost2._"),
        (r) => `${r.oldKost2} → ${r.newKost2}`,
        220
      ),
    ],
    [t]
  );
  const table = <T,>(columns: ColumnDef<T, unknown>[], data: T[]) => (
    <DataTable<T>
      columns={columns}
      data={data}
      enableColumnFilters={false}
      manualSorting={false}
      getRowId={(_, i) => String(i)}
    />
  );

  return (
    <div className="flex flex-col gap-3">
      {plan.errors.map((error, i) => (
        <FormAlert key={`error-${i}`} tone="error">
          {error}
        </FormAlert>
      ))}
      {plan.warnings.map((warning, i) => (
        <FormAlert key={i} tone="warning">
          {warning}
        </FormAlert>
      ))}
      <h3 className="text-sm font-semibold">
        {t("fibu.kost2Reallocation.sums")}
      </h3>
      {table(sums, plan.sums)}
      <h3 className="text-sm font-semibold">
        {t("fibu.buchungssaetze")} ({plan.records.length})
      </h3>
      {table(records, plan.records)}
      <h3 className="text-sm font-semibold">
        {t("fibu.kost2Reallocation.timesheets")} ({plan.timesheets.length})
      </h3>
      {table(moves, plan.timesheets)}
    </div>
  );
}
