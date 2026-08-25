"use client";

import { useMemo } from "react";
import { useTranslations } from "next-intl";
import type { ColumnDef, RowSelectionState } from "@tanstack/react-table";
import {
  DataTable,
  selectionColumn,
  SELECTION_COLUMN_ID,
} from "@/components/data-table";
import { ImportDiffCell } from "./import-diff-cell";
import { ImportStatusCell } from "./import-status-cell";
import {
  isSelectable,
  rowClassForStatus,
  visibleColumns,
} from "./import-model";
import type { ImportColumn, ImportConfig, ImportEntry } from "./import-types";

interface Props {
  config: ImportConfig;
  entries: ImportEntry[];
  meta: Record<string, unknown>;
  selection: RowSelectionState;
  onSelectionChange: (next: RowSelectionState) => void;
  isFetching?: boolean;
}

/**
 * The preview of an import, driven entirely by [ImportConfig]: a status column, a ticking column for the
 * importable rows and one column per configured field, each tinted by its row's reconciliation state.
 * Because the columns are the config's, this same table serves the incoming-invoice import today and the
 * address/banking imports later — nothing here knows the entity.
 */
export function ImportPreviewTable({
  config,
  entries,
  meta,
  selection,
  onSelectionChange,
  isFetching,
}: Props) {
  const t = useTranslations();
  const shown = useMemo(
    () => visibleColumns(config.columns, meta),
    [config.columns, meta]
  );

  const columns = useMemo<ColumnDef<ImportEntry, unknown>[]>(() => {
    const statusColumn: ColumnDef<ImportEntry, unknown> = {
      id: "status",
      header: t("status"),
      size: 150,
      enableSorting: false,
      meta: { label: t("status"), wrap: true },
      cell: ({ row }) => <ImportStatusCell entry={row.original} />,
    };
    const fieldColumns = shown.map<ColumnDef<ImportEntry, unknown>>(
      (column: ImportColumn) => ({
        id: column.field,
        header: t(column.headerKey),
        size: column.width ?? 140,
        enableSorting: false,
        meta: { label: t(column.headerKey), wrap: true },
        cell: ({ row }) => (
          <ImportDiffCell entry={row.original} column={column} />
        ),
      })
    );
    return [selectionColumn<ImportEntry>(), statusColumn, ...fieldColumns];
  }, [shown, t]);

  return (
    <DataTable<ImportEntry>
      columns={columns}
      data={entries}
      isFetching={isFetching}
      getRowId={(row) => String(row.id)}
      enableColumnFilters={false}
      manualSorting={false}
      showPagination={false}
      lockedColumnIds={[SELECTION_COLUMN_ID]}
      enableRowSelection={(row) =>
        isSelectable(row.original.status, config.selectableStatuses)
      }
      rowSelection={selection}
      onRowSelectionChange={(updater) =>
        onSelectionChange(
          typeof updater === "function" ? updater(selection) : updater
        )
      }
      rowClassName={(row) => rowClassForStatus(row.status)}
      className="flex-1"
    />
  );
}
