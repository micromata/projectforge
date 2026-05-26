"use client";

import Link from "next/link";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Copy01Icon,
  Delete01Icon,
  Edit02Icon,
  SortingIcon,
} from "@hugeicons/core-free-icons";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";
import type { Book } from "./types";

interface BooksTableProps {
  data: Book[];
  sortCol: string | null;
  onSort: (col: string) => void;
}

const COLS: { id: keyof Book; label: string; w?: string; flex?: boolean }[] = [
  { id: "angelegt", label: "Angelegt", w: "100px" },
  { id: "jahr", label: "Jahr", w: "70px" },
  { id: "sig", label: "Signatur", w: "90px" },
  { id: "autor", label: "Autor:innen", w: "170px" },
  { id: "titel", label: "Titel", flex: true },
  { id: "key", label: "Schlüsselworte", w: "160px" },
  { id: "ausgelBy", label: "Ausgeliehen von", w: "150px" },
];

export function BooksTable({ data, sortCol, onSort }: BooksTableProps) {
  return (
    <Table className="text-sm">
      <TableHeader className="bg-muted/40">
        <TableRow>
          {COLS.map((col) => {
            const active = sortCol === col.id;
            return (
              <TableHead
                key={col.id}
                onClick={() => onSort(col.id)}
                className={cn(
                  "cursor-pointer select-none text-[11px] font-bold uppercase tracking-wider transition-colors",
                  active
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:bg-muted/60"
                )}
                style={{ width: col.w, minWidth: col.w }}
              >
                <span className="inline-flex items-center gap-1">
                  {col.label}
                  {active && (
                    <HugeiconsIcon
                      icon={SortingIcon}
                      size={12}
                      className="text-primary"
                    />
                  )}
                </span>
              </TableHead>
            );
          })}
          <TableHead style={{ width: "90px" }} />
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.map((row) => (
          <TableRow key={row.id} className="group relative">
            {/* Hover accent bar */}
            <td
              aria-hidden
              className="pointer-events-none absolute inset-y-0 left-0 w-[3px] bg-primary opacity-0 transition-opacity group-hover:opacity-100"
            />
            <TableCell className="text-muted-foreground">
              {row.angelegt}
            </TableCell>
            <TableCell className="font-medium">{row.jahr}</TableCell>
            <TableCell>
              <span className="inline-flex items-center rounded-sm border border-border bg-muted px-1.5 py-0.5 font-mono text-[11px] font-semibold text-foreground/80">
                {row.sig}
              </span>
            </TableCell>
            <TableCell className="font-medium">{row.autor}</TableCell>
            <TableCell>
              <Link
                href={`/books/${row.id}`}
                className="block truncate font-semibold text-primary hover:underline"
              >
                {row.titel}
              </Link>
            </TableCell>
            <TableCell className="text-muted-foreground">{row.key}</TableCell>
            <TableCell>
              {row.avail ? (
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
                  ● {row.ausgelBy}
                </span>
              )}
            </TableCell>
            <TableCell>
              <div className="flex items-center justify-end gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                <button
                  type="button"
                  className="flex size-7 items-center justify-center rounded-sm bg-primary/10 text-primary hover:bg-primary/20"
                  aria-label="Bearbeiten"
                >
                  <HugeiconsIcon icon={Edit02Icon} size={13} />
                </button>
                <button
                  type="button"
                  className="flex size-7 items-center justify-center rounded-sm bg-primary/10 text-primary hover:bg-primary/20"
                  aria-label="Kopieren"
                >
                  <HugeiconsIcon icon={Copy01Icon} size={13} />
                </button>
                <button
                  type="button"
                  className="flex size-7 items-center justify-center rounded-sm hover:opacity-90"
                  style={{
                    background: "var(--status-loaned-bg)",
                    color: "var(--status-loaned)",
                  }}
                  aria-label="Löschen"
                >
                  <HugeiconsIcon icon={Delete01Icon} size={13} />
                </button>
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
