"use client";

import { useState } from "react";
import type { Column } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  const inputType =
    mode === "date" ? "date" : mode === "number" ? "number" : "text";

  function apply() {
    if (!needsValue) {
      column.setFilterValue({ type: mode, operator });
      return;
    }
    if (value === "") {
      column.setFilterValue(undefined);
      return;
    }
    const parse = (v: string) => (mode === "number" ? Number(v) : v);
    column.setFilterValue({
      type: mode,
      operator,
      value: parse(value),
      ...(needsRange && valueTo !== "" ? { valueTo: parse(valueTo) } : {}),
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
        <Input
          type={inputType}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && apply()}
          placeholder={t("value")}
          className="h-7 text-xs"
          aria-label={t("value")}
        />
      )}
      {needsRange && (
        <Input
          type={inputType}
          value={valueTo}
          onChange={(e) => setValueTo(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && apply()}
          placeholder={t("valueTo")}
          className="h-7 text-xs"
          aria-label={t("valueTo")}
        />
      )}
      <Button size="sm" className="h-7 w-full text-xs" onClick={apply}>
        {t("apply")}
      </Button>
    </div>
  );
}
