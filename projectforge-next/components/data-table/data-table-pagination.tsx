"use client";

import type { Table } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { PAGE_SIZE_OPTIONS } from "./page-size-options";

interface DataTablePaginationProps<TData> {
  table: Table<TData>;
  pageSizeOptions?: number[];
}

export function DataTablePagination<TData>({
  table,
  pageSizeOptions = PAGE_SIZE_OPTIONS,
}: DataTablePaginationProps<TData>) {
  const t = useTranslations("table");
  const { pageIndex, pageSize } = table.getState().pagination;
  const total = table.getRowCount();
  const from = total === 0 ? 0 : pageIndex * pageSize + 1;
  const to = Math.min(total, (pageIndex + 1) * pageSize);
  const pageCount = table.getPageCount();

  const label = t("range", { from, to, total });
  // A page can set a size of its own (the address import uses 500), and a size stored for the user may
  // predate the current list - so the active one is always offered, or the select would show blank.
  const sizes = pageSizeOptions.includes(pageSize)
    ? pageSizeOptions
    : [...pageSizeOptions, pageSize].sort((a, b) => a - b);

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
          aria-label={t("previousPage")}
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
          aria-label={t("nextPage")}
        >
          <HugeiconsIcon icon={ArrowRight01Icon} size={13} />
        </Button>
      </div>
      <div className="flex items-center gap-2">
        <span className="text-xs text-muted-foreground">
          {t("rowsPerPage")}
        </span>
        <select
          className="h-7 rounded-sm border bg-background px-2 text-xs"
          value={pageSize}
          onChange={(e) => table.setPageSize(Number(e.target.value))}
        >
          {sizes.map((s) => (
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
