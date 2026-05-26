"use client";

import type { Table } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface DataTablePaginationProps<TData> {
  table: Table<TData>;
  pageSizeOptions?: number[];
  totalLabel?: (range: { from: number; to: number; total: number }) => string;
}

export function DataTablePagination<TData>({
  table,
  pageSizeOptions = [25, 50, 100],
  totalLabel,
}: DataTablePaginationProps<TData>) {
  const { pageIndex, pageSize } = table.getState().pagination;
  const total = table.getRowCount();
  const from = total === 0 ? 0 : pageIndex * pageSize + 1;
  const to = Math.min(total, (pageIndex + 1) * pageSize);
  const pageCount = table.getPageCount();

  const label = totalLabel
    ? totalLabel({ from, to, total })
    : `${from}–${to} von ${total} Einträgen`;

  return (
    <div className="flex items-center justify-between border-t px-4 py-2">
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      <div className="flex items-center gap-1">
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="size-7"
          onClick={() => table.previousPage()}
          disabled={!table.getCanPreviousPage()}
          aria-label="Vorherige Seite"
        >
          <HugeiconsIcon icon={ArrowLeft01Icon} size={13} />
        </Button>
        {pageNumbers(pageIndex, pageCount).map((p, i) =>
          p === "…" ? (
            <span
              key={`ellipsis-${i}`}
              className="px-1 text-xs text-muted-foreground"
            >
              …
            </span>
          ) : (
            <button
              key={p}
              type="button"
              onClick={() => table.setPageIndex(p - 1)}
              className={cn(
                "h-7 min-w-7 rounded-sm border px-2 text-xs font-medium",
                p - 1 === pageIndex
                  ? "border-primary bg-primary text-primary-foreground"
                  : "bg-background text-muted-foreground hover:bg-muted"
              )}
            >
              {p}
            </button>
          )
        )}
        <Button
          type="button"
          variant="outline"
          size="icon"
          className="size-7"
          onClick={() => table.nextPage()}
          disabled={!table.getCanNextPage()}
          aria-label="Nächste Seite"
        >
          <HugeiconsIcon icon={ArrowRight01Icon} size={13} />
        </Button>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-xs text-muted-foreground">Einträge / Seite:</span>
        <select
          className="h-7 rounded-sm border bg-background px-2 text-xs"
          value={pageSize}
          onChange={(e) => table.setPageSize(Number(e.target.value))}
        >
          {pageSizeOptions.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>
    </div>
  );
}

function pageNumbers(pageIndex: number, pageCount: number): (number | "…")[] {
  if (pageCount <= 1) return [1];
  const current = pageIndex + 1;
  const out: (number | "…")[] = [];
  const push = (n: number | "…") => {
    if (out[out.length - 1] !== n) out.push(n);
  };
  push(1);
  if (current - 1 > 2) push("…");
  for (
    let p = Math.max(2, current - 1);
    p <= Math.min(pageCount - 1, current + 1);
    p++
  ) {
    push(p);
  }
  if (current + 1 < pageCount - 1) push("…");
  if (pageCount > 1) push(pageCount);
  return out;
}
