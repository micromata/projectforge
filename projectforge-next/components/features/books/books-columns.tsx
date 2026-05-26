"use client";

import type { ColumnDef } from "@tanstack/react-table";
import Link from "next/link";
import { DataTableColumnHeader } from "@/components/data-table";
import type { Book } from "./types";

export const booksColumns: ColumnDef<Book>[] = [
  {
    accessorKey: "angelegt",
    size: 100,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Angelegt</DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="text-muted-foreground">{getValue<string>()}</span>
    ),
  },
  {
    accessorKey: "jahr",
    size: 70,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Jahr</DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="font-medium">{getValue<string>()}</span>
    ),
  },
  {
    accessorKey: "sig",
    size: 90,
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
    accessorKey: "autor",
    size: 170,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Autor:innen</DataTableColumnHeader>
    ),
    cell: ({ getValue }) => (
      <span className="font-medium">{getValue<string>()}</span>
    ),
  },
  {
    accessorKey: "titel",
    minSize: 240,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>Titel</DataTableColumnHeader>
    ),
    cell: ({ row }) => (
      <Link
        href={`/books/${row.original.id}`}
        className="block truncate font-semibold text-primary hover:underline"
      >
        {row.original.titel}
      </Link>
    ),
  },
  {
    accessorKey: "key",
    size: 160,
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
    accessorKey: "ausgelBy",
    size: 170,
    header: ({ column }) => (
      <DataTableColumnHeader column={column}>
        Ausgeliehen von
      </DataTableColumnHeader>
    ),
    cell: ({ row }) =>
      row.original.avail ? (
        <span
          className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap"
          style={{
            background: "var(--status-available-bg)",
            color: "var(--status-available)",
            borderColor: "var(--status-available-border)",
          }}
        >
          ● Verfügbar
        </span>
      ) : (
        <span
          className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap"
          style={{
            background: "var(--status-loaned-bg)",
            color: "var(--status-loaned)",
            borderColor: "var(--status-loaned-border)",
          }}
        >
          ● {row.original.ausgelBy}
        </span>
      ),
  },
];
