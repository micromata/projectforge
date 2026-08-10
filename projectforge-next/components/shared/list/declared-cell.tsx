"use client";

import type { ReactNode } from "react";
import {
  formatDate,
  formatTimestampMinutes,
  type FormatContext,
} from "@/lib/format";
import { cn } from "@/lib/utils";
import type { FieldMetadata } from "@/lib/metadata/types";

export interface DeclaredCellContext {
  format: FormatContext;
  /** Translator without a namespace, for the constants of an enum field. */
  t: (key: string) => string;
  className?: string;
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
  { format, t, className }: DeclaredCellContext
): ReactNode {
  if (value == null || value === "") return null;

  const enumValue = field.enumValues?.find((v) => v.value === value);
  if (enumValue) {
    return (
      <span className={className}>
        {enumValue.i18nKey ? t(enumValue.i18nKey) : enumValue.value}
      </span>
    );
  }

  switch (field.dataType) {
    case "TIMESTAMP":
      return (
        <span className={cn("text-muted-foreground", className)}>
          {formatTimestampMinutes(value, format)}
        </span>
      );
    case "DATE":
      return (
        <span className={cn("text-muted-foreground", className)}>
          {formatDate(value, format)}
        </span>
      );
    default:
      return <span className={className}>{String(value)}</span>;
  }
}
