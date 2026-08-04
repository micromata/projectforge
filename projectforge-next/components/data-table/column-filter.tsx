"use client";

import { useMemo, useState } from "react";
import type { Column } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toFilterText, type ColumnFilterValue } from "./filter-fns";

/** Filter kind, derived from the column meta the backend sends. */
export type FilterKind = "text" | "number" | "date";

interface ColumnFilterProps<TData> {
  column: Column<TData, unknown>;
  kind: FilterKind;
}

type Mode = "selection" | "text" | "number" | "date";

function initialMode(
  value: ColumnFilterValue | undefined,
  kind: FilterKind
): Mode {
  if (Array.isArray(value)) return "selection";
  if (value?.type) return value.type;
  return kind === "text" ? "selection" : kind;
}

export function ColumnFilter<TData>({
  column,
  kind,
}: ColumnFilterProps<TData>) {
  const t = useTranslations("filter");
  const current = column.getFilterValue() as ColumnFilterValue | undefined;
  const [mode, setMode] = useState<Mode>(() => initialMode(current, kind));

  return (
    <div className="w-64 p-2">
      <Select value={mode} onValueChange={(v) => setMode(v as Mode)}>
        <SelectTrigger size="sm" className="mb-2 w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value="selection">{t("selection")}</SelectItem>
          {kind === "text" && <SelectItem value="text">{t("text")}</SelectItem>}
          {kind === "number" && (
            <SelectItem value="number">{t("number")}</SelectItem>
          )}
          {kind === "date" && <SelectItem value="date">{t("date")}</SelectItem>}
        </SelectContent>
      </Select>

      {mode === "selection" ? (
        <SelectionFilter column={column} />
      ) : (
        <ComparisonFilter column={column} mode={mode} />
      )}

      {column.getIsFiltered() && (
        <Button
          variant="ghost"
          size="sm"
          className="mt-2 w-full text-xs"
          onClick={() => column.setFilterValue(undefined)}
        >
          {t("reset")}
        </Button>
      )}
    </div>
  );
}

/** Checkbox list of the distinct values present in the column. */
function SelectionFilter<TData>({
  column,
}: {
  column: Column<TData, unknown>;
}) {
  const t = useTranslations("filter");
  const [search, setSearch] = useState("");
  const selected = column.getFilterValue();

  const values = useMemo(() => {
    const unique = new Set<string>();
    for (const raw of column.getFacetedUniqueValues().keys()) {
      // Array cells contribute each entry separately.
      if (Array.isArray(raw)) raw.forEach((v) => unique.add(toFilterText(v)));
      else unique.add(toFilterText(raw));
    }
    return [...unique].sort((a, b) => {
      if (a === "") return 1; // blanks last
      if (b === "") return -1;
      return a.localeCompare(b);
    });
  }, [column]);

  const accepted = Array.isArray(selected) ? selected : values;
  const shown = search
    ? values.filter((v) => v.toLowerCase().includes(search.toLowerCase()))
    : values;

  function toggle(value: string, checked: boolean) {
    const next = new Set(accepted);
    if (checked) next.add(value);
    else next.delete(value);
    // "everything selected" is the same as "no filter" — keeps the state clean.
    column.setFilterValue(next.size === values.length ? undefined : [...next]);
  }

  return (
    <div>
      <div className="relative mb-2">
        <HugeiconsIcon
          icon={Search01Icon}
          size={13}
          className="absolute left-2 top-1/2 -translate-y-1/2 text-muted-foreground"
        />
        <Input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t("search")}
          className="h-7 pl-7 text-xs"
          aria-label={t("search")}
        />
      </div>
      <div className="max-h-52 space-y-0.5 overflow-y-auto">
        {shown.length === 0 && (
          <p className="px-1 py-2 text-xs text-muted-foreground">
            {t("emptyValue")}
          </p>
        )}
        {shown.map((value) => (
          <label
            key={value}
            className="flex cursor-pointer items-center gap-2 rounded-sm px-1 py-1 text-xs hover:bg-accent"
          >
            <Checkbox
              checked={accepted.includes(value)}
              onCheckedChange={(checked) => toggle(value, checked === true)}
            />
            <span className="truncate">
              {value === "" ? t("blank") : value}
            </span>
          </label>
        ))}
      </div>
      <div className="mt-2 flex gap-1">
        <Button
          variant="outline"
          size="sm"
          className="h-6 flex-1 text-xs"
          onClick={() => column.setFilterValue(undefined)}
        >
          {t("selectAll")}
        </Button>
        <Button
          variant="outline"
          size="sm"
          className="h-6 flex-1 text-xs"
          onClick={() => column.setFilterValue([])}
        >
          {t("selectNone")}
        </Button>
      </div>
    </div>
  );
}

/** Operator keys per filter kind; labels come from the dataTable.operators messages. */
const OPERATORS: Record<"text" | "number" | "date", string[]> = {
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
function operatorKey(
  mode: "text" | "number" | "date",
  operator: string
): string {
  if (mode !== "date") return operator;
  if (operator === "equals") return "dateEquals";
  if (operator === "notEqual") return "dateNotEqual";
  return operator;
}

/** Operator + value input(s) for text, number and date columns. */
function ComparisonFilter<TData>({
  column,
  mode,
}: {
  column: Column<TData, unknown>;
  mode: "text" | "number" | "date";
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
