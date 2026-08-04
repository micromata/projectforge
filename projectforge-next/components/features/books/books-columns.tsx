"use client";

import type { ColumnDef } from "@tanstack/react-table";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { DataTableColumnHeader } from "@/components/data-table";
import { StatusBadge } from "./status-badge";
import type { BookListRow } from "./types";

/**
 * Column defs as a hook: the header labels come from the backend's i18n bundle
 * (see GenerateNextI18nMessagesMain), which needs the translation context.
 */
export function useBooksColumns(): ColumnDef<BookListRow>[] {
  const t = useTranslations();
  const tBook = useTranslations("book");

  return [
  {
    accessorKey: "created",
    meta: { label: t("created") },
    size: 84,
    header: ({ column }) => (
      <DataTableColumnHeader column={column} filterKind="date">
        {t("created")}
      </DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="text-muted-foreground">{getValue<string>()}</span>
    ),
  },
  {
    accessorKey: "yearOfPublishing",
    meta: { label: tBook("yearOfPublishing.short") },
    size: 56,
    header: ({ column }) => (
      <DataTableColumnHeader column={column} filterKind="number">
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
    header: ({ column }) => (
      <DataTableColumnHeader column={column} filterKind="text">
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
    header: ({ column }) => (
      <DataTableColumnHeader column={column} filterKind="text">
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
    header: ({ column }) => (
      <DataTableColumnHeader column={column} filterKind="text">
        {tBook("title._")}
      </DataTableColumnHeader>
    ),
    cell: ({ row }) => (
      <Link
        href={`/books/${row.original.id}`}
        className="block truncate font-semibold text-primary hover:underline"
      >
        {row.original.title}
      </Link>
    ),
  },
  {
    accessorKey: "keywords",
    meta: { label: tBook("keywords") },
    size: 132,
    header: ({ column }) => (
      <DataTableColumnHeader column={column} filterKind="text">
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
    header: ({ column }) => (
      <DataTableColumnHeader column={column} filterKind="text">
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
  ];
}
