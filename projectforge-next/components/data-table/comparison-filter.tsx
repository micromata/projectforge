"use client";

import { useState } from "react";
import type { Column } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { DateInput } from "@/components/shared/date-input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { ColumnFilterValue, FilterKind } from "./column-filter-types";

/** Operator keys per filter kind; labels come from the filter messages. */
const OPERATORS: Record<FilterKind, string[]> = {
  text: [
    "contains",
    "notContains",
    "equals",
    "startsWith",
    "endsWith",
    "blank",
    "notBlank",
  ],
  number: [
    "equals",
    "notEqual",
    "greaterThan",
    "lessThan",
    "between",
    "blank",
    "notBlank",
  ],
  date: [
    "equals",
    "notEqual",
    "before",
    "after",
    "between",
    "blank",
    "notBlank",
  ],
};

/** Date columns read "on"/"not on" rather than "equals"/"not equal". */
function operatorKey(mode: FilterKind, operator: string): string {
  if (mode !== "date") return operator;
  if (operator === "equals") return "dateEquals";
  if (operator === "notEqual") return "dateNotEqual";
  return operator;
}

/**
 * The one value of a comparison, in the control its kind needs. A date is entered through the shared
 * [DateInput] rather than a native date field: it keeps emitting `YYYY-MM-DD` (which is what
 * filter-fns.ts compares lexicographically) while showing and accepting the user's own layout.
 */
function ValueInput({
  mode,
  value,
  onChange,
  onSubmit,
  label,
}: {
  mode: FilterKind;
  value: string;
  onChange: (value: string) => void;
  /** Called on Enter, with the value as it stands at that moment (see [ComparisonFilter.apply]). */
  onSubmit: (value: string) => void;
  label: string;
}) {
  if (mode === "date") {
    return (
      <DateInput
        value={value || null}
        onChange={(next) => onChange(next ?? "")}
        onSubmit={(committed) => onSubmit(committed ?? "")}
        aria-label={label}
      />
    );
  }
  return (
    <Input
      type={mode === "number" ? "number" : "text"}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      onKeyDown={(e) => e.key === "Enter" && onSubmit(value)}
      placeholder={label}
      className="h-7 text-xs"
      aria-label={label}
    />
  );
}

/** Operator + value input(s) for text, number and date columns. */
export function ComparisonFilter<TData>({
  column,
  mode,
}: {
  column: Column<TData, unknown>;
  mode: FilterKind;
}) {
  const t = useTranslations("filter");
  const current = column.getFilterValue() as ColumnFilterValue | undefined;
  const active =
    current && !Array.isArray(current) && current.type === mode
      ? current
      : undefined;

  const [operator, setOperator] = useState<string>(
    active?.operator ?? OPERATORS[mode][0]
  );
  const [value, setValue] = useState<string>(
    active && "value" in active && active.value != null
      ? String(active.value)
      : ""
  );
  const [valueTo, setValueTo] = useState<string>(
    active && "valueTo" in active && active.valueTo != null
      ? String(active.valueTo)
      : ""
  );

  const needsValue = operator !== "blank" && operator !== "notBlank";
  const needsRange = operator === "between";

  /**
   * The values are passed in rather than read from state: [DateInput] commits on Enter and calls
   * `onSubmit` right after, so the state of this render is still the previous date.
   */
  function apply(from = value, to = valueTo) {
    if (!needsValue) {
      column.setFilterValue({ type: mode, operator });
      return;
    }
    if (from === "") {
      column.setFilterValue(undefined);
      return;
    }
    const parse = (v: string) => (mode === "number" ? Number(v) : v);
    column.setFilterValue({
      type: mode,
      operator,
      value: parse(from),
      ...(needsRange && to !== "" ? { valueTo: parse(to) } : {}),
    });
  }

  return (
    <div className="space-y-2">
      <Select value={operator} onValueChange={setOperator}>
        <SelectTrigger size="sm" className="w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {OPERATORS[mode].map((op) => (
            <SelectItem key={op} value={op}>
              {t(operatorKey(mode, op))}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      {needsValue && (
        <ValueInput
          mode={mode}
          value={value}
          onChange={setValue}
          onSubmit={(committed) => apply(committed)}
          label={t("value")}
        />
      )}
      {needsRange && (
        <ValueInput
          mode={mode}
          value={valueTo}
          onChange={setValueTo}
          onSubmit={(committed) => apply(value, committed)}
          label={t("valueTo")}
        />
      )}
      {/* Wrapped, so the click event isn't taken for a value. */}
      <Button size="sm" className="h-7 w-full text-xs" onClick={() => apply()}>
        {t("apply")}
      </Button>
    </div>
  );
}
