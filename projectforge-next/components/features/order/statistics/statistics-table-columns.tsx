"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import type { ColumnDef, Row } from "@tanstack/react-table";
import {
  formatCurrency,
  formatDate,
  formatNumber,
  type FormatContext,
} from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * Column builders of the data tables of the order statistics (forecast and contribution margin): the
 * tables are read like the sheets of their Excel exports, so they share the look of their cells — amounts
 * right-aligned with a red minus, dates and links — and a sum under the amount columns.
 *
 * Each builder names the filter its column offers (see StatisticsTable). A missing value stays undefined, so
 * the filters see it as blank and sorting puts it last.
 */

type Col<T> = ColumnDef<T, unknown>;

/** An amount, red if negative; `className` e.g. marks a preliminary value. */
export function Money({
  value,
  ctx,
  className,
}: {
  value: number | null | undefined;
  ctx: FormatContext;
  className?: string;
}) {
  if (value == null) return null;
  return (
    <span
      className={cn("tabular-nums", value < 0 && "text-destructive", className)}
    >
      {formatCurrency(value, ctx)}
    </span>
  );
}

/** The sum of `value` over the given rows, e.g. those left by the column filters. */
export function sumOfFiltered<T>(
  rows: Row<T>[],
  value: (row: T) => number | null | undefined
): number {
  return rows.reduce((acc, row) => acc + (value(row.original) ?? 0), 0);
}

export function textColumn<T>(
  id: string,
  label: string,
  value: (row: T) => string | null | undefined,
  size = 160,
  footer?: string
): Col<T> {
  return {
    id,
    accessorFn: (row) => value(row) ?? "",
    header: label,
    size,
    meta: { label, filterKind: "text" },
    cell: ({ row }) => value(row.original) ?? "",
    footer,
  };
}

/**
 * An amount column, summed up in the footer over the rows left by the column filters (all pages, not only
 * the current one). `className` styles single cells, e.g. preliminary values.
 */
export function moneyColumn<T>(
  id: string,
  label: string,
  value: (row: T) => number | null | undefined,
  ctx: FormatContext,
  options: {
    size?: number;
    sum?: boolean;
    className?: (row: T) => string | undefined;
  } = {}
): Col<T> {
  const { size = 120, sum = true, className } = options;
  return {
    id,
    accessorFn: (row) => value(row) ?? undefined,
    header: label,
    size,
    sortDescFirst: true,
    sortUndefined: "last",
    meta: { label, align: "right", filterKind: "number" },
    cell: ({ row }) => (
      <Money
        value={value(row.original)}
        ctx={ctx}
        className={className?.(row.original)}
      />
    ),
    footer: sum
      ? ({ table }) => (
          <Money
            value={sumOfFiltered(table.getFilteredRowModel().rows, value)}
            ctx={ctx}
          />
        )
      : undefined,
  };
}

/** A plain number (person days, hours, probability), right-aligned and not summed. */
export function numberColumn<T>(
  id: string,
  label: string,
  value: (row: T) => number | null | undefined,
  ctx: FormatContext,
  size = 90,
  fractionDigits = 2
): Col<T> {
  return {
    id,
    accessorFn: (row) => value(row) ?? undefined,
    header: label,
    size,
    sortDescFirst: true,
    sortUndefined: "last",
    meta: { label, align: "right", filterKind: "number" },
    cell: ({ row }) => (
      <span className="tabular-nums">
        {formatNumber(value(row.original), ctx, fractionDigits)}
      </span>
    ),
  };
}

export function dateColumn<T>(
  id: string,
  label: string,
  value: (row: T) => string | null | undefined,
  ctx: FormatContext,
  size = 100
): Col<T> {
  return {
    id,
    accessorFn: (row) => value(row) ?? undefined,
    header: label,
    size,
    sortUndefined: "last",
    meta: { label, filterKind: "date" },
    cell: ({ row }) => formatDate(value(row.original), ctx),
  };
}

/**
 * A text linking to `href` of the row (e.g. the order or invoice); plain text without one. An `action`
 * (e.g. an icon button opening details) stands at the right edge of the cell.
 */
export function linkColumn<T>(
  id: string,
  label: string,
  value: (row: T) => string | number | null | undefined,
  href: (row: T) => string | null,
  size = 90,
  action?: (row: T) => ReactNode
): Col<T> {
  return {
    id,
    accessorFn: (row) => value(row) ?? "",
    header: label,
    size,
    meta: { label, filterKind: "text" },
    cell: ({ row }) => {
      const text = value(row.original);
      const target = href(row.original);
      if (text == null || text === "") return null;
      const content = target ? (
        <Link href={target} className="hover:underline">
          {text}
        </Link>
      ) : (
        text
      );
      const extra = action?.(row.original);
      return extra ? (
        <span className="flex w-full items-center justify-between gap-1">
          {content}
          {extra}
        </span>
      ) : (
        content
      );
    },
  };
}
