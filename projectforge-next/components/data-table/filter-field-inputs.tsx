"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DateInput } from "@/components/shared/date-input";
import type { MagicFilterEntryValue } from "@/lib/rs/types";
import { fromLikeTerm, toLikeTerm } from "./filter-value";

/** What every input in here needs; `element` is only added where the options matter. */
export interface FilterInputProps {
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
  label: string;
  id: string;
  /** Focus on mount, so a filter opened from the pill row is ready to type into. */
  autoFocus?: boolean;
  /**
   * Enter in a single-line input; used by the pill popover to save and close. A field that changes
   * its value in the same handler (see [RangeField]) passes the value to save along, because the
   * `onChange` above has not reached the caller's state yet by then.
   */
  onSubmit?: (value?: MagicFilterEntryValue | undefined) => void;
}

/**
 * @param raw Send the term as typed, without the wildcards a LIKE query needs. For the full-text
 *   fields: `DBHistoryQuery.searchHistoryEntryByFullTextQuery` appends the `*` itself, and a
 *   Lucene term already wrapped in them matches nothing.
 */
export function TextField({
  value,
  onChange,
  label,
  id,
  autoFocus,
  onSubmit,
  raw,
}: FilterInputProps & { raw?: boolean }) {
  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <Input
        id={`filter-${id}`}
        autoFocus={autoFocus}
        value={raw ? (value?.value ?? "") : fromLikeTerm(value?.value)}
        onChange={(e) => {
          const term = raw ? e.target.value.trim() : toLikeTerm(e.target.value);
          onChange(term === "" ? undefined : { value: term });
        }}
        onKeyDown={(e) => {
          if (e.key === "Enter" && onSubmit) {
            e.preventDefault();
            onSubmit();
          }
        }}
        className="h-8 text-xs"
      />
    </div>
  );
}

export function BooleanField({ value, onChange, label, id }: FilterInputProps) {
  return (
    <label
      htmlFor={`filter-${id}`}
      className="flex cursor-pointer items-center gap-2 text-xs"
    >
      <Checkbox
        id={`filter-${id}`}
        checked={value?.value === "true"}
        onCheckedChange={(checked) =>
          onChange(checked === true ? { value: "true" } : undefined)
        }
      />
      <span>{label}</span>
    </label>
  );
}

export function RangeField({
  value,
  onChange,
  label,
  autoFocus,
  onSubmit,
}: FilterInputProps) {
  const t = useTranslations("filter");

  function next(part: "from" | "to", raw: string | null) {
    const merged = { ...value, [part]: raw ?? undefined };
    return merged.from || merged.to ? merged : undefined;
  }

  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">{label}</p>
      <div className="space-y-1">
        <DateInput
          autoFocus={autoFocus}
          aria-label={`${label}: ${t("value")}`}
          value={value?.from}
          defaultMonth={value?.to}
          onChange={(iso) => onChange(next("from", iso))}
          // The date [DateInput] just committed, since `value` here is still the previous one.
          onSubmit={(iso) => onSubmit?.(next("from", iso))}
        />
        <DateInput
          aria-label={`${label}: ${t("valueTo")}`}
          value={value?.to}
          // Opens in the month of the range's start while the end is still empty.
          defaultMonth={value?.from}
          onChange={(iso) => onChange(next("to", iso))}
          onSubmit={(iso) => onSubmit?.(next("to", iso))}
        />
      </div>
    </div>
  );
}
