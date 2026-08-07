"use client";

import { useMemo } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";
import { DataTableColumnHeader } from "@/components/data-table";
import { StatusBadge } from "./status-badge";
import type { BookListRow } from "./types";

/**
 * Column defs as a hook: the header labels come from the backend's i18n bundle
 * (see GenerateNextI18nMessagesMain), which needs the translation context.
 *
 * Memoised because the identity matters: a new array makes TanStack rebuild every
 * column instance, and the new `header` function identity remounts the header —
 * which closes an open filter popover on the very click that opened it.
 */
export function useBooksColumns(): ColumnDef<BookListRow>[] {
  const t = useTranslations();
  const tBook = useTranslations("book");
  const formatCtx = useFormatContext();

  return useMemo<ColumnDef<BookListRow>[]>(
    () => [
      {
        accessorKey: "created",
        meta: { label: t("created") },
        // Wide enough for a localised date and time.
        size: 130,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="date"
          >
            {t("created")}
          </DataTableColumnHeader>
        ),
        cell: ({ getValue }) => (
          <span className="text-muted-foreground">
            {formatTimestampMinutes(getValue<string>(), formatCtx)}
          </span>
        ),
      },
      {
        accessorKey: "yearOfPublishing",
        meta: { label: tBook("yearOfPublishing.short") },
        size: 56,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="number"
          >
            {tBook("yearOfPublishing.short")}
          </DataTableColumnHeader>
        ),
        cell: ({ getValue }) => (
          <span className="font-medium">{getValue<string>()}</span>
        ),
      },
      {
        accessorKey: "signature",
        meta: { label: tBook("signature") },
        size: 76,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="text"
          >
            {tBook("signature")}
          </DataTableColumnHeader>
        ),
        cell: ({ getValue }) => (
          <span className="inline-flex items-center rounded-sm border border-border bg-muted px-1.5 py-0.5 font-mono text-[11px] font-semibold text-foreground/80">
            {getValue<string>()}
          </span>
        ),
      },
      {
        accessorKey: "authors",
        meta: { label: tBook("authors") },
        size: 140,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="text"
          >
            {tBook("authors")}
          </DataTableColumnHeader>
        ),
        cell: ({ getValue }) => (
          <span className="font-medium">{getValue<string>()}</span>
        ),
      },
      {
        accessorKey: "title",
        meta: { label: tBook("title._") },
        // Explicit size: the fixed table layout ignores minSize, so without it the
        // column would fall back to TanStack's 150px default.
        size: 280,
        minSize: 200,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="text"
          >
            {tBook("title._")}
          </DataTableColumnHeader>
        ),
        // No link: the whole row navigates to the edit page (see books/page.tsx).
        cell: ({ getValue }) => (
          <span className="font-semibold text-primary">
            {getValue<string>()}
          </span>
        ),
      },
      {
        accessorKey: "keywords",
        meta: { label: tBook("keywords") },
        size: 132,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="text"
          >
            {tBook("keywords")}
          </DataTableColumnHeader>
        ),
        cell: ({ getValue }) => (
          <span className="text-muted-foreground">{getValue<string>()}</span>
        ),
      },
      {
        id: "lendOutBy",
        meta: { label: tBook("lendOutBy") },
        accessorFn: (row) => row.lendOutBy?.displayName ?? "",
        size: 140,
        header: ({ column, table }) => (
          <DataTableColumnHeader
            column={column}
            table={table}
            filterKind="text"
          >
            {tBook("lendOutBy")}
          </DataTableColumnHeader>
        ),
        cell: ({ row }) => {
          const borrower = row.original.lendOutBy;
          return borrower ? (
            <StatusBadge lendOut label={borrower.displayName} />
          ) : (
            <StatusBadge lendOut={false} label={t("book.status.present")} />
          );
        },
      },
    ],
    [t, tBook, formatCtx]
  );
}
