"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import type { Kost2ReallocationTimesheet } from "@/lib/rs/kost2-reallocation";

/** The time sheets of a run. */
export function TimesheetsTable({
  rows,
}: {
  rows: Kost2ReallocationTimesheet[];
}) {
  const t = useTranslations();
  const format = useFormatContext();
  const columns = useMemo<
    ColumnDef<Kost2ReallocationTimesheet, unknown>[]
  >(() => {
    const col = (
      id: string,
      label: string,
      cell: (row: Kost2ReallocationTimesheet) => React.ReactNode,
      size: number
    ): ColumnDef<Kost2ReallocationTimesheet, unknown> => ({
      id,
      header: label,
      size,
      enableSorting: false,
      meta: { label },
      cell: ({ row }) => cell(row.original),
    });
    return [
      col("user", t("timesheet.user"), (r) => r.user, 160),
      col(
        "start",
        t("timesheet.startTime"),
        (r) => formatTimestampMinutes(r.startTime, format),
        150
      ),
      col("hours", t("hours"), (r) => r.hours.toFixed(2), 70),
      col("task", t("task._"), (r) => r.task, 300),
      col("kost2", t("fibu.kost2._"), (r) => r.kost2, 110),
      col("description", t("description"), (r) => r.description, 300),
    ];
  }, [t, format]);

  return (
    <DataTable<Kost2ReallocationTimesheet>
      columns={columns}
      data={rows}
      enableColumnFilters={false}
      manualSorting={false}
      getRowId={(row) => String(row.id)}
    />
  );
}
