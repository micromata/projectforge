"use client";

import { useMemo } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import type { ColumnDef } from "@tanstack/react-table";
import { DataTable } from "@/components/data-table";
import { ImportDiffCell } from "@/components/shared/import/import-diff-cell";
import { rowClassForStatus } from "@/components/shared/import/import-model";
import { ImportStatusCell } from "@/components/shared/import/import-status-cell";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  addressImportImageUrl,
  type AddressImportEntry,
} from "@/lib/rs/address-import";
import { ADDRESS_IMPORT_COLUMNS } from "./address-import-columns";

/** Large enough to hold any import on one page. */
const ALL_ROWS_PAGE_SIZE = 100_000;

/**
 * The entries of the import. A click opens the entry in the address form — the stored address it matches, or a
 * new one — which compares the fields of the vCard with the stored ones; saving it there marks the entry as
 * imported. An imported entry stays clickable, for a second look.
 */
export function AddressImportTable({
  entries,
}: {
  entries: AddressImportEntry[];
}) {
  const t = useTranslations();
  const router = useRouter();
  const columns = useMemo<ColumnDef<AddressImportEntry, unknown>[]>(() => {
    const status: ColumnDef<AddressImportEntry, unknown> = {
      id: "status",
      header: t("status"),
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

  return (
    <DataTable<AddressImportEntry>
      columns={columns}
      data={entries}
      getRowId={(row) => String(row.id)}
      enableColumnFilters={false}
      manualSorting={false}
      showPagination={false}
      initialPageSize={ALL_ROWS_PAGE_SIZE}
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
  );
}
