"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { useTranslations } from "next-intl";
import { DataTableColumnHeader } from "@/components/data-table";
import { HighlightedText } from "@/components/shared/highlighted-text";
import { TaskConsumptionBar } from "@/components/shared/tasks/task-consumption-bar";
import type { TaskConsumption } from "@/lib/rs/task";
import type { TimesheetDetail } from "../../types";

type T = ReturnType<typeof useTranslations>;
type Column = ColumnDef<TimesheetDetail, unknown>;

/**
 * The columns of the recent table, in reading order — cost unit, its customer and project (all on the
 * `cost2Visible` gate of the legacy table, since all three only exist for a cost-unit booking), then task,
 * consumption, location, description, tag and reference. Each text column is filterable and sortable over
 * the text it shows, and highlights the search term in it.
 */
export function recentColumns(t: T, cost2Visible: boolean): Column[] {
  const text = (
    id: string,
    label: string,
    size: number,
    value: (entry: TimesheetDetail) => string
  ): Column => ({
    id,
    accessorFn: value,
    header: function RecentColumnHeader({ column, table }) {
      return (
        <DataTableColumnHeader column={column} table={table} filterKind="text">
          {label}
        </DataTableColumnHeader>
      );
    },
    size,
    meta: { label },
    cell: ({ getValue, table }) => (
      <HighlightedText
        text={String(getValue() ?? "")}
        query={table.options.meta?.highlight}
      />
    ),
  });
  const consumptionLabel = t("task.consumption");
  const columns: (Column | false)[] = [
    cost2Visible &&
      text("kost2", t("fibu.kost2._"), 120, (e) => e.kost2?.displayName ?? ""),
    cost2Visible &&
      text(
        "customer",
        t("fibu.kunde._"),
        130,
        (e) => e.kost2?.project?.customer?.name ?? ""
      ),
    cost2Visible &&
      text(
        "project",
        t("fibu.projekt._"),
        130,
        (e) => e.kost2?.project?.name ?? ""
      ),
    text("task", t("task._"), 170, taskName),
    {
      // The budget's bar, as the form shows it once the entry is picked; not a link here, the row picks.
      // Sorted by the booked share, entries without a budget last; not filtered, its texts are figures.
      id: "consumption",
      accessorFn: (e) => consumptionOf(e)?.percentage,
      sortUndefined: "last",
      enableColumnFilter: false,
      header: function RecentConsumptionHeader({ column, table }) {
        return (
          <DataTableColumnHeader column={column} table={table}>
            {consumptionLabel}
          </DataTableColumnHeader>
        );
      },
      size: 90,
      meta: { label: consumptionLabel },
      cell: ({ row }) =>
        row.original.task ? (
          <TaskConsumptionBar
            task={{
              consumption: row.original.task.consumption,
              title: taskName(row.original),
            }}
            linkEnabled={false}
          />
        ) : null,
    },
    text("location", t("timesheet.location"), 110, (e) => e.location ?? ""),
    text("description", t("description"), 160, (e) => e.description ?? ""),
    text("tag", t("timesheet.tag"), 100, (e) => e.tag ?? ""),
    text("reference", t("timesheet.reference"), 110, (e) => e.reference ?? ""),
  ];
  return columns.filter((c): c is Column => !!c);
}

/** The structure element's name without its trailing task id — "Neukundenakquise (#25207467)" → "Neukundenakquise". */
export function taskName(entry: TimesheetDetail): string {
  return (entry.task?.displayName ?? "").replace(/\s*\(#\d+\)\s*$/, "");
}

function consumptionOf(entry: TimesheetDetail): TaskConsumption | undefined {
  return entry.task?.consumption as TaskConsumption | undefined;
}

/** The consumption of the entry's budget as the backend wrote it out — "12,5PT/20PT (62%)" — or "". */
export function consumptionTitle(entry: TimesheetDetail): string {
  return consumptionOf(entry)?.title ?? "";
}

/**
 * The one lower-cased string a row is matched against — every field it shows (see filterRecent, legacy),
 * the consumption included, so a search for "PT" or a percentage keeps the entries with a budget.
 */
export function searchText(entry: TimesheetDetail): string {
  return [
    entry.task?.displayName,
    consumptionTitle(entry),
    entry.kost2?.displayName,
    entry.kost2?.project?.customer?.name,
    entry.kost2?.project?.name,
    entry.location,
    entry.tag,
    entry.reference,
    entry.description,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}
