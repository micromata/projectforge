"use client";

import { DateInput } from "@/components/shared/date-input";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Label } from "@/components/ui/label";

/**
 * A date control of the order statistics (start, planning date) on one line, label before the input: the
 * controls sit in a single row right under the tabs, so the charts start as high as possible.
 */
export function StatisticsDateField({
  id,
  label,
  tooltip,
  value,
  onChange,
}: {
  id: string;
  label: string;
  tooltip: string;
  value: string | null;
  onChange: (value: string | null) => void;
}) {
  return (
    <div className="flex items-center gap-2">
      <HintTooltip text={tooltip} openOnTap>
        <Label htmlFor={id} className="whitespace-nowrap">
          {label}:
        </Label>
      </HintTooltip>
      <DateInput id={id} value={value} onChange={onChange} aria-label={label} />
    </div>
  );
}
