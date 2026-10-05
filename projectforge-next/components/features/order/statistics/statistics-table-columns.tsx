"use client";

import Link from "next/link";
import type { ColumnDef } from "@tanstack/react-table";
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
    meta: { label },
    cell: ({ row }) => value(row.original) ?? "",
    footer,
  };
}

/**
 * An amount column, summed up in the footer over all rows (not only those of the current page).
 * `className` styles single cells, e.g. preliminary values.
 */
export function moneyColumn<T>(
  id: string,
  label: string,
  value: (row: T) => number | null | undefined,
  rows: T[],
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
    accessorFn: (row) => value(row) ?? Number.NEGATIVE_INFINITY,
    header: label,
    size,
    sortDescFirst: true,
    meta: { label, align: "right" },
    cell: ({ row }) => (
      <Money
        value={value(row.original)}
        ctx={ctx}
        className={className?.(row.original)}
      />
    ),
    footer: sum
      ? () => (
          <Money
            value={rows.reduce((acc, row) => acc + (value(row) ?? 0), 0)}
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
    accessorFn: (row) => value(row) ?? Number.NEGATIVE_INFINITY,
    header: label,
    size,
    sortDescFirst: true,
    meta: { label, align: "right" },
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
    accessorFn: (row) => value(row) ?? "",
    header: label,
    size,
    meta: { label },
    cell: ({ row }) => formatDate(value(row.original), ctx),
  };
}

/** A text linking to `href` of the row (e.g. the order or invoice); plain text without one. */
export function linkColumn<T>(
  id: string,
  label: string,
  value: (row: T) => string | number | null | undefined,
  href: (row: T) => string | null,
  size = 90
): Col<T> {
  return {
    id,
    accessorFn: (row) => value(row) ?? "",
    header: label,
    size,
    meta: { label },
    cell: ({ row }) => {
      const text = value(row.original);
      const target = href(row.original);
      if (text == null || text === "") return null;
      return target ? (
        <Link href={target} className="hover:underline">
          {text}
        </Link>
      ) : (
        text
      );
    },
  };
}
