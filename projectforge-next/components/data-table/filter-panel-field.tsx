"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";

interface FilterPanelFieldProps {
  element: FilterElement;
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
}

/**
 * One input per backend filter field, chosen by its filterType.
 *
 * OBJECT fields (entity lookup via autoCompletion) fall back to a plain text
 * input for now — a proper autocomplete needs its own component.
 */
export function FilterPanelField({
  element,
  value,
  onChange,
}: FilterPanelFieldProps) {
  const label = element.label ?? element.id;

  switch (element.filterType) {
    case "LIST":
      return <ListField element={element} value={value} onChange={onChange} label={label} />;
    case "BOOLEAN":
      return <BooleanField value={value} onChange={onChange} label={label} id={element.id} />;
    case "DATE":
    case "TIMESTAMP":
      return <RangeField value={value} onChange={onChange} label={label} id={element.id} />;
    default:
      return <TextField value={value} onChange={onChange} label={label} id={element.id} />;
  }
}

/**
 * Wraps the term in wildcards: the backend turns a STRING entry into a LIKE
 * predicate that matches the whole field otherwise ("Larkin" finds nothing when
 * the value is "Peter J. Larkin"). Terms that already carry a wildcard are left
 * alone so users can anchor a search themselves.
 */
function toLikeTerm(input: string): string {
  const term = input.trim();
  if (term === "") return "";
  return term.includes("*") ? term : `*${term}*`;
}

/** Strips the wildcards again so the input shows what the user typed. */
function fromLikeTerm(stored: string | undefined): string {
  if (!stored) return "";
  const match = /^\*(.*)\*$/.exec(stored);
  return match ? match[1] : stored;
}

function TextField({
  value,
  onChange,
  label,
  id,
}: {
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
  label: string;
  id: string;
}) {
  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <Input
        id={`filter-${id}`}
        value={fromLikeTerm(value?.value)}
        onChange={(e) => {
          const term = toLikeTerm(e.target.value);
          onChange(term === "" ? undefined : { value: term });
        }}
        className="h-8 text-xs"
      />
    </div>
  );
}

function ListField({
  element,
  value,
  onChange,
  label,
}: {
  element: FilterElement;
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
  label: string;
}) {
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
      <div className="space-y-0.5">
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

function BooleanField({
  value,
  onChange,
  label,
  id,
}: {
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
  label: string;
  id: string;
}) {
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

function RangeField({
  value,
  onChange,
  label,
  id,
}: {
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
  label: string;
  id: string;
}) {
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
      <input type="hidden" id={`filter-${id}`} />
    </div>
  );
}
