"use client";

import { useState } from "react";
import type { Column } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { useDistinctFilterValues } from "./use-distinct-filter-values";

/**
 * Checkbox list of the distinct values present in the column.
 *
 * Mounted only while the selection mode is active — building the list is the
 * expensive part of the filter popover, so it must not run for a column the user
 * only wants to compare against.
 */
export function SelectionFilter<TData>({
  column,
}: {
  column: Column<TData, unknown>;
}) {
  const t = useTranslations("filter");
  const [search, setSearch] = useState("");
  const selected = column.getFilterValue();
  const values = useDistinctFilterValues(column);

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
