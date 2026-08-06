"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import { fromLikeTerm, toLikeTerm } from "./filter-value";

/** What every input in here needs; `element` is only added where the options matter. */
export interface FilterInputProps {
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
  label: string;
  id: string;
  /** Focus on mount, so a filter opened from the pill row is ready to type into. */
  autoFocus?: boolean;
  /** Enter in a single-line input; used by the pill popover to save and close. */
  onSubmit?: () => void;
}

export function TextField({
  value,
  onChange,
  label,
  id,
  autoFocus,
  onSubmit,
}: FilterInputProps) {
  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <Input
        id={`filter-${id}`}
        autoFocus={autoFocus}
        value={fromLikeTerm(value?.value)}
        onChange={(e) => {
          const term = toLikeTerm(e.target.value);
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

export function ListField({
  element,
  value,
  onChange,
  label,
}: FilterInputProps & { element: FilterElement }) {
  const selected = value?.values ?? [];

  function toggle(id: string, checked: boolean) {
    // Single-select fields replace their value instead of accumulating.
    const next = element.multi
      ? checked
        ? [...selected, id]
        : selected.filter((v) => v !== id)
      : checked
        ? [id]
        : [];
    onChange(next.length ? { values: next } : undefined);
  }

  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">{label}</p>
      <div className="max-h-48 space-y-0.5 overflow-y-auto">
        {element.values?.map((option) => (
          <label
            key={option.id}
            className="flex cursor-pointer items-center gap-2 rounded-sm px-1 py-0.5 text-xs hover:bg-accent"
          >
            <Checkbox
              checked={selected.includes(option.id)}
              onCheckedChange={(checked) => toggle(option.id, checked === true)}
            />
            <span className="truncate">{option.displayName}</span>
          </label>
        ))}
      </div>
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
}: FilterInputProps) {
  const t = useTranslations("filter");

  function update(part: "from" | "to", raw: string) {
    const next = { ...value, [part]: raw || undefined };
    onChange(next.from || next.to ? next : undefined);
  }

  return (
    <div className="space-y-1">
      <p className="text-xs font-medium">{label}</p>
      <div className="flex gap-1">
        <Input
          type="date"
          autoFocus={autoFocus}
          aria-label={`${label}: ${t("value")}`}
          value={value?.from ?? ""}
          onChange={(e) => update("from", e.target.value)}
          className="h-8 text-xs"
        />
        <Input
          type="date"
          aria-label={`${label}: ${t("valueTo")}`}
          value={value?.to ?? ""}
          onChange={(e) => update("to", e.target.value)}
          className="h-8 text-xs"
        />
      </div>
    </div>
  );
}
