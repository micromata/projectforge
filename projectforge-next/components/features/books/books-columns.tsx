"use client";

import type { ColumnDef } from "@tanstack/react-table";
import Link from "next/link";
import { DataTableColumnHeader } from "@/components/data-table";
import { StatusBadge } from "./status-badge";
import type { BookListRow } from "./types";

export const booksColumns: ColumnDef<BookListRow>[] = [
  {
    accessorKey: "created",
    size: 84,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Angelegt</DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="text-muted-foreground">{getValue<string>()}</span>
    ),
  },
  {
    accessorKey: "yearOfPublishing",
    size: 56,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Jahr</DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="font-medium">{getValue<string>()}</span>
    ),
  },
  {
    accessorKey: "signature",
    size: 76,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Signatur</DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="inline-flex items-center rounded-sm border border-border bg-muted px-1.5 py-0.5 font-mono text-[11px] font-semibold text-foreground/80">
        {getValue<string>()}
      </span>
    ),
  },
  {
    accessorKey: "authors",
    size: 140,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Autor:innen</DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="font-medium">{getValue<string>()}</span>
    ),
  },
  {
    accessorKey: "title",
    minSize: 200,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Titel</DataTableColumnHeader>
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
    size: 132,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>
        Schlüsselworte
      </DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="text-muted-foreground">{getValue<string>()}</span>
    ),
  },
  {
    id: "lendOutBy",
    accessorFn: (row) => row.lendOutBy?.displayName ?? "",
    size: 140,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>
        Ausgeliehen von
      </DataTableColumnHeader>
    ),
    cell: ({ row }) => {
      const borrower = row.original.lendOutBy;
      return borrower ? (
        <StatusBadge lendOut label={borrower.displayName} />
      ) : (
        <StatusBadge lendOut={false} label="Verfügbar" />
      );
    },
  },
];
