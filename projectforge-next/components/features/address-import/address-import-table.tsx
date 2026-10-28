"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import {
  DataTable,
  DataTableColumnHeader,
  DataTableColumnPanel,
  useColumnStatePersistence,
  useDataTable,
  useStoredColumnState,
  useTableState,
  type ColumnState,
} from "@/components/data-table";
import { ImportDiffCell } from "@/components/shared/import/import-diff-cell";
import { rowClassForStatus } from "@/components/shared/import/import-model";
import { ImportStatusCell } from "@/components/shared/import/import-status-cell";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  addressImportImageUrl,
  type AddressImportEntry,
} from "@/lib/rs/address-import";
import { ADDRESS_IMPORT_COLUMNS } from "./address-import-columns";

/** The user prefs of the table (`AddressImportRest.columnStates` / `setColumnStates`). */
const GRID = "addressImport";

/**
 * The entries of the import. A click opens the entry in the address form — the stored address it matches, or a
 * new one — which compares the fields of the vCard with the stored ones; saving it there marks the entry as
 * imported. An imported entry stays clickable, for a second look.
 *
 * The status is filterable, the columns are stored in the user's prefs as the React page had it (rendered once
 * the stored state has arrived, see CachesTable).
 */
export function AddressImportTable({
  entries,
}: {
  entries: AddressImportEntry[];
}) {
  const stored = useStoredColumnState(GRID);
  if (stored.isPending) return null;
  return <LoadedTable entries={entries} storedState={stored.data ?? {}} />;
}

function LoadedTable({
  entries,
  storedState,
}: {
  entries: AddressImportEntry[];
  storedState: ColumnState;
}) {
  const t = useTranslations();
  const router = useRouter();
  const columns = useMemo<ColumnDef<AddressImportEntry, unknown>[]>(() => {
    const status: ColumnDef<AddressImportEntry, unknown> = {
      id: "status",
      accessorFn: (row) => row.statusAsString,
      header: function StatusHeader({ column, table }) {
        return (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="text"
          >
            {t("status")}
          </DataTableColumnHeader>
        );
      },
      size: 140,
      enableSorting: false,
      meta: { label: t("status"), wrap: true },
      cell: ({ row }) => <ImportStatusCell entry={row.original} />,
    };
    const image: ColumnDef<AddressImportEntry, unknown> = {
      id: "image",
      header: t("address.image"),
      size: 72,
      enableSorting: false,
      meta: { label: t("address.image") },
      cell: ({ row }) =>
        row.original.hasImage ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={addressImportImageUrl(row.original.id)}
            alt=""
            loading="lazy"
            className="max-h-12 max-w-16 rounded object-contain"
          />
        ) : null,
    };
    const fields = ADDRESS_IMPORT_COLUMNS.map<
      ColumnDef<AddressImportEntry, unknown>
    >((column) => {
      const label = t(leafKeyOf(column.headerKey, t.has));
      return {
        id: column.field,
        header: label,
        size: column.width ?? 140,
        enableSorting: false,
        meta: { label, wrap: true },
        cell: ({ row }) => (
          <ImportDiffCell entry={row.original} column={column} />
        ),
      };
    });
    const additional: ColumnDef<AddressImportEntry, unknown> = {
      id: "additionalChanges",
      header: t("common.import.additionalChanges"),
      size: 220,
      enableSorting: false,
      meta: { label: t("common.import.additionalChanges"), wrap: true },
      cell: ({ row }) => row.original.additionalChanges ?? null,
    };
    return [
      status,
      ...fields.slice(0, 3),
      image,
      ...fields.slice(3),
      additional,
    ];
  }, [t]);

  const state = useTableState({ restoredState: storedState });
  const table = useDataTable<AddressImportEntry>({
    columns,
    data: entries,
    sorting: state.sorting,
    onSortingChange: state.setSorting,
    columnFilters: state.columnFilters,
    onColumnFiltersChange: state.setColumnFilters,
    columnVisibility: state.columnVisibility,
    onColumnVisibilityChange: state.setColumnVisibility,
    columnPinning: state.columnPinning,
    onColumnPinningChange: state.setColumnPinning,
    columnSizing: state.columnSizing,
    onColumnSizingChange: state.setColumnSizing,
    columnOrder: state.columnOrder,
    onColumnOrderChange: state.setColumnOrder,
    enableColumnFilters: true,
    enableColumnResizing: true,
    manualPagination: true,
    getRowId: (row) => String(row.id),
  });
  useColumnStatePersistence(GRID, {
    sorting: state.sorting,
    columnVisibility: state.columnVisibility,
    columnPinning: state.columnPinning,
    columnSizing: state.columnSizing,
    columnOrder: state.columnOrder,
  });
  // Back to the columns as declared; the persistence then stores the empty state.
  const resetColumns = () => {
    state.setSorting([]);
    state.setColumnVisibility({});
    state.setColumnPinning({});
    state.setColumnSizing({});
    state.setColumnOrder([]);
    state.setColumnFilters([]);
  };

  return (
    <>
      <div className="flex justify-end">
        <DataTableColumnPanel table={table} onReset={resetColumns} />
      </div>
      <DataTable<AddressImportEntry>
        table={table}
        columns={[]}
        data={[]}
        showPagination={false}
        rowClassName={(row) =>
          row.status === "IMPORTED" ? "row-grey" : rowClassForStatus(row.status)
        }
        onRowClick={(row) =>
          router.push(
            row.addressId != null
              ? `/address/${row.addressId}/edit?importIndex=${row.id}`
              : `/address/new?importIndex=${row.id}`
          )
        }
        className="flex-1"
      />
    </>
  );
}
