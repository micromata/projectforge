"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { DataTable } from "@/components/data-table";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { VACATION_METADATA } from "@/lib/metadata/vacation.generated";
import type { VacationDetail } from "./types";
import { useVacationStats } from "./use-vacation-stats";

type Row = VacationDetail & { index: number };

const PINNED = { left: ["employee"] };

/**
 * The vacations of the replacements overlapping this one — the React form's grid below the fields, which
 * tells why there is a conflict and whom to pick instead. Dates and days as the backend formatted them.
 */
export function VacationsOfReplacements() {
  const t = useTranslations();
  const label = useFieldLabels(VACATION_METADATA);
  const stats = useVacationStats();
  const rows = useMemo<Row[]>(
    () =>
      (stats?.conflictingVacations ?? []).map((vacation, index) => ({
        ...vacation,
        index,
      })),
    [stats?.conflictingVacations]
  );
  // Strings, not the label function, as the columns' dependency: useFieldLabels answers a new function on
  // every render, which would rebuild the columns every time.
  const headers = {
    employee: label("employee"),
    startDate: label("startDate"),
    endDate: label("endDate"),
    vacationMode: t("vacation.vacationmode"),
    status: label("status"),
    replacement: label("replacement"),
    otherReplacements: t("vacation.replacement.others"),
    manager: label("manager"),
    workingDays: t("vacation.workingdays"),
  };
  const headerKey = JSON.stringify(headers);
  const columns = useMemo<ColumnDef<Row, unknown>[]>(() => {
    const h = JSON.parse(headerKey) as typeof headers;
    const column = (
      id: keyof typeof headers,
      value: (row: Row) => string | null | undefined,
      size = 150
    ): ColumnDef<Row, unknown> => ({
      id,
      header: h[id],
      size,
      enableSorting: false,
      meta: { label: h[id] },
      cell: ({ row }) => value(row.original) ?? "",
    });
    return [
      column("employee", (r) => r.employee?.displayName),
      column("startDate", (r) => r.startDateFormatted, 100),
      column("endDate", (r) => r.endDateFormatted, 100),
      column("vacationMode", (r) => r.vacationModeString, 110),
      column("status", (r) => r.statusString, 110),
      column("replacement", (r) => r.replacement?.displayName),
      column("otherReplacements", (r) =>
        (r.otherReplacements ?? []).map((e) => e.displayName).join(", ")
      ),
      column("manager", (r) => r.manager?.displayName),
      column("workingDays", (r) => r.workingDaysFormatted, 90),
    ];
  }, [headerKey]);
  // Scrolls sideways in its own box, as the chart above it does: an autoHeight table leaves the scrolling to the
  // page, which would let its columns run out of the section.
  return (
    <div className="min-w-0 overflow-x-auto">
      <DataTable
        columns={columns}
        data={rows}
        enableColumnFilters={false}
        manualSorting={false}
        showPagination={false}
        dense
        autoHeight
        // The employee stays at the left while the other columns scroll.
        columnPinning={PINNED}
        getRowId={(row) => String(row.id ?? row.index)}
        emptyState={
          <p className="text-xs text-muted-foreground">{t("nothingFound")}</p>
        }
      />
    </div>
  );
}
