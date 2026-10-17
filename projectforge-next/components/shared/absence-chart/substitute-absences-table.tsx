"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { DataTable } from "@/components/data-table";

/** A vacation or an availability entry of a substitute, as the table shows it (texts formatted by the backend). */
export interface SubstituteAbsenceRow {
  id: string;
  name?: string | null;
  startDateFormatted?: string | null;
  endDateFormatted?: string | null;
  /** E.g. the vacation's mode, or "Availability" for an availability entry. */
  kind?: string | null;
  /** The vacation's status, or what the availability entry is called for the user. */
  status?: string | null;
  /** The values of the [extraColumns], by their ids. */
  extra?: Record<string, string | null | undefined>;
}

export interface SubstituteAbsenceColumn {
  id: string;
  header: string;
  size?: number;
}

const PINNED = { left: ["name"] };

/**
 * The absences of the substitutes overlapping the entry being edited — its chart's text alternative and what tells
 * why there is a conflict and whom to pick instead: name, period, kind and status, then the [extraColumns] (e.g. a
 * vacation's replacements and manager). Not "nothing found" if the stats failed ([unavailableText]): that would read
 * as "no conflict".
 */
export function SubstituteAbsencesTable({
  rows,
  headers,
  extraColumns = [],
  unavailableText,
}: {
  rows: SubstituteAbsenceRow[];
  headers: {
    name: string;
    startDate: string;
    endDate: string;
    kind: string;
    status: string;
  };
  extraColumns?: SubstituteAbsenceColumn[];
  /** Shown instead of the rows if the stats failed. */
  unavailableText?: string | null;
}) {
  const t = useTranslations();
  // Strings, not objects, as the columns' dependency: the callers build new ones on every render.
  const columnsKey = JSON.stringify({ headers, extraColumns });
  const columns = useMemo<ColumnDef<SubstituteAbsenceRow, unknown>[]>(() => {
    const { headers: h, extraColumns: extra } = JSON.parse(columnsKey) as {
      headers: typeof headers;
      extraColumns: SubstituteAbsenceColumn[];
    };
    const column = (
      id: string,
      header: string,
      value: (row: SubstituteAbsenceRow) => string | null | undefined,
      size = 150
    ): ColumnDef<SubstituteAbsenceRow, unknown> => ({
      id,
      header,
      size,
      enableSorting: false,
      meta: { label: header },
      cell: ({ row }) => value(row.original) ?? "",
    });
    return [
      column("name", h.name, (r) => r.name),
      column("startDate", h.startDate, (r) => r.startDateFormatted, 100),
      column("endDate", h.endDate, (r) => r.endDateFormatted, 100),
      column("kind", h.kind, (r) => r.kind, 110),
      column("status", h.status, (r) => r.status, 110),
      ...extra.map((c) =>
        column(c.id, c.header, (r) => r.extra?.[c.id], c.size)
      ),
    ];
  }, [columnsKey]);
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
        // The name stays at the left while the other columns scroll.
        columnPinning={PINNED}
        getRowId={(row) => row.id}
        emptyState={
          unavailableText ? (
            <p className="text-xs text-destructive">{unavailableText}</p>
          ) : (
            <p className="text-xs text-muted-foreground">{t("nothingFound")}</p>
          )
        }
      />
    </div>
  );
}
