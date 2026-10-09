"use client";

import type { ReactNode } from "react";
import { DateInput } from "@/components/shared/date-input";
import { FieldHint } from "@/components/shared/form/field-hint";
import { Label } from "@/components/ui/label";

/**
 * A date control of the order statistics (start, planning date) on one line, label before the input: the
 * controls sit in a single row right under the tabs, so the charts start as high as possible.
 */
export function StatisticsDateField({
  id,
  label,
  tooltip,
  tooltipContent,
  value,
  onChange,
}: {
  id: string;
  label: string;
  tooltip: string;
  /** A structured tooltip replacing [tooltip], e.g. [tooltip] followed by a configured hint. */
  tooltipContent?: ReactNode;
  value: string | null;
  onChange: (value: string | null) => void;
}) {
  return (
    <div className="flex items-center gap-2">
      <div className="flex items-center gap-1">
        <Label htmlFor={id} className="whitespace-nowrap">
          {label}
        </Label>
        <FieldHint hint={tooltip} content={tooltipContent} label={label} />
        <span aria-hidden>:</span>
      </div>
      <DateInput id={id} value={value} onChange={onChange} aria-label={label} />
    </div>
  );
}
