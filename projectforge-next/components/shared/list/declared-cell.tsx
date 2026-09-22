"use client";

import type { ReactNode } from "react";
import {
  formatCurrency,
  formatDate,
  formatNumber,
  formatTimestampMinutes,
  type FormatContext,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import { HighlightedText } from "@/components/shared/highlighted-text";
import type { FieldMetadata } from "@/lib/metadata/types";

export interface DeclaredCellContext {
  format: FormatContext;
  /** Translator without a namespace, for the constants of an enum field. */
  t: (key: string) => string;
  className?: string;
  /** The active search term, highlighted wherever it matched the cell text (see HighlightedText). */
  highlight?: string;
}

/**
 * How a value renders when the declaration says nothing about it — derived from the field's data
 * type, so a timestamp is a timestamp in the user's time zone and an enum shows the text the legacy
 * pages show rather than its constant name.
 *
 * Everything else is text. A column that needs more (a badge, a link, an icon) declares its own
 * `cell`; this is the floor, not a formatter registry.
 */
export function declaredCell(
  value: unknown,
  field: FieldMetadata,
  { format, t, className, highlight }: DeclaredCellContext
): ReactNode {
  if (value == null || value === "") return null;

  // Highlights the search match in the cell text, in every branch (see HighlightedText). Renders the
  // text untouched when there is no term, so it can wrap unconditionally.
  const mark = (text: string) => (
    <HighlightedText text={text} query={highlight} />
  );

  const enumValue = field.enumValues?.find((v) => v.value === value);
  if (enumValue) {
    return (
      <span className={className}>
        {mark(enumValue.i18nKey ? t(enumValue.i18nKey) : enumValue.value)}
      </span>
    );
  }

  switch (field.dataType) {
    case "TIMESTAMP":
      return (
        <span className={cn("text-muted-foreground tabular-nums", className)}>
          {mark(formatTimestampMinutes(value, format))}
        </span>
      );
    case "DATE":
      return (
        <span className={cn("text-muted-foreground tabular-nums", className)}>
          {mark(formatDate(value, format))}
        </span>
      );
    // An AMOUNT is money and carries the user's currency; a DECIMAL is a plain quantity (person days),
    // which the entities declare with two digits as well (`@Column(scale = 2)`).
    case "AMOUNT":
      return (
        <span className={cn("font-mono tabular-nums", className)}>
          {mark(formatCurrency(value, format))}
        </span>
      );
    case "DECIMAL":
      return (
        <span className={cn("font-mono tabular-nums", className)}>
          {mark(formatNumber(value, format, 2))}
        </span>
      );
    // Whole numbers line up in their column like the decimals do; only the digit grouping differs.
    case "INT":
    case "LONG":
      return (
        <span className={cn("font-mono tabular-nums", className)}>
          {mark(String(value))}
        </span>
      );
    default:
      return <span className={className}>{mark(String(value))}</span>;
  }
}
