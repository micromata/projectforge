"use client";

import type { Table } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { formatNumber } from "@/lib/format";
import { useFormatContext } from "@/hooks/use-format";
import { PAGE_SIZE_OPTIONS } from "./page-size-options";
import { PAGE_GAP, pageSlots } from "./page-slots";

interface DataTablePaginationProps<TData> {
  table: Table<TData>;
  pageSizeOptions?: number[];
}

export function DataTablePagination<TData>({
  table,
  pageSizeOptions = PAGE_SIZE_OPTIONS,
}: DataTablePaginationProps<TData>) {
  const t = useTranslations("table");
  const formatCtx = useFormatContext();
  const { pageIndex, pageSize } = table.getState().pagination;
  const total = table.getRowCount();
  const from = total === 0 ? 0 : pageIndex * pageSize + 1;
  const to = Math.min(total, (pageIndex + 1) * pageSize);
  const pageCount = table.getPageCount();

  // Formatted here, not left to the message's ICU argument: the numbers are the user's and must
  // carry their grouping separator ("17.152"), which comes from userData, not from the UI locale.
  const label = t("range", {
    from: formatNumber(from, formatCtx),
    to: formatNumber(to, formatCtx),
    total: formatNumber(total, formatCtx),
  });
  // A page can set a size of its own (the address import uses 500), and a size stored for the user may
  // predate the current list - so the active one is always offered, or the select would show blank.
  const sizes = pageSizeOptions.includes(pageSize)
    ? pageSizeOptions
    : [...pageSizeOptions, pageSize].sort((a, b) => a - b);

  return (
    // Three columns rather than `justify-between`: the two outer ones are equally wide whatever they
    // hold, so the strip in the middle stays where it is when the range label grows a digit ("51-100"
    // after "1-50"). With `min-w-0` and a truncated label, because a label wider than its share would
    // push the middle aside again.
    <div className="grid grid-cols-[1fr_auto_1fr] items-center border-t px-4 py-2">
      <span className="min-w-0 truncate text-xs font-medium text-muted-foreground tabular-nums">
        {label}
      </span>
      <div
        className="flex items-center justify-center gap-1"
        // Every slot is as wide as the widest page number of *this* list, so a step from 9 to 10 does
        // not widen the strip either (see `.pagination-slot` in globals.css).
        style={
          {
            "--pagination-digits": String(pageCount).length,
          } as React.CSSProperties
        }
      >
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
        {pageSlots(pageIndex, pageCount).map((slot, i) =>
          slot === PAGE_GAP ? (
            // As wide as a page button and in its place, so the strip's length does not depend on how
            // many of its slots are pages (see pageSlots).
            <span
              key={`gap-${i}`}
              aria-hidden
              className="pagination-slot h-7 text-center text-xs leading-7 text-muted-foreground"
            >
              {PAGE_GAP}
            </span>
          ) : (
            <button
              key={slot}
              type="button"
              onClick={() => table.setPageIndex(slot - 1)}
              className={cn(
                "pagination-slot h-7 rounded-sm border px-2 text-xs font-medium",
                slot - 1 === pageIndex
                  ? "border-primary bg-primary text-primary-foreground"
                  : "bg-background text-muted-foreground hover:bg-muted"
              )}
            >
              {slot}
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
      <div className="flex min-w-0 items-center justify-end gap-2">
        <span className="truncate text-xs text-muted-foreground">
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
