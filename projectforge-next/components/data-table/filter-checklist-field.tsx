"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Checklist } from "@/components/shared/checklist";
import { OverflowPills } from "@/components/shared/overflow-pills";
import type { FilterElement } from "@/lib/rs/types";
import type { FilterInputProps } from "./filter-field-inputs";
import { useFilterListValues } from "./use-filter-list-values";

/**
 * A LIST filter whose values the backend loads on demand (`valuesUrl`), e.g. the customers of the order
 * book: picked from an Excel-like [Checklist], since there are far too many of them to scroll a plain
 * option list.
 *
 * In a pill popover (`inline`) the checklist lies open; in the "all filters" dialog the field stays one
 * line — the picks as [OverflowPills] — and the checklist opens below it.
 *
 * It offers the values of the current result only (it narrows it further), unless "choose from all" is
 * switched on: then those of every row the user may see. The switch is not kept, it starts off each time.
 */
export function FilterChecklistField({
  element,
  value,
  onChange,
  label,
  id,
  autoFocus,
  inline,
}: FilterInputProps & { element: FilterElement; inline?: boolean }) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const [fromAll, setFromAll] = useState(false);
  const switchId = useId();
  const selected = value?.values ?? [];
  // Needed to list them, and to name the picks in the closed field.
  const { values, loading } = useFilterListValues(
    element,
    inline || open || selected.length > 0,
    fromAll
  );
  const options = values.map((it) => ({
    value: it.id,
    label: it.displayName,
    freeText: it.freeText,
    group: it.group,
    members: it.members,
  }));
  // An emptied field is dropped from the filter, as everywhere else.
  const onValues = (next: string[]) =>
    onChange(next.length ? { values: next } : undefined);

  const checklist = (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <Switch
          id={switchId}
          size="sm"
          checked={fromAll}
          onCheckedChange={setFromAll}
        />
        <Label htmlFor={switchId} className="text-xs font-normal">
          {t("select.checklist.fromAll")}
        </Label>
      </div>
      <Checklist
        aria-label={label}
        options={options}
        selected={selected}
        onChange={onValues}
        loading={loading}
        autoFocus={autoFocus || !inline}
        hint={t(
          fromAll
            ? "select.checklist.fromAllHint"
            : "select.checklist.narrowsHint"
        )}
        emptyText={
          fromAll ? undefined : t("select.checklist.noneForOtherFilters")
        }
      />
    </div>
  );

  if (inline) {
    return (
      <div className="space-y-1">
        <p className="text-xs">{label}</p>
        {checklist}
      </div>
    );
  }

  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <Popover
        open={open}
        onOpenChange={(next) => {
          setOpen(next);
          if (!next) setFromAll(false);
        }}
      >
        <PopoverTrigger asChild>
          <Button
            id={`filter-${id}`}
            variant="outline"
            aria-label={label}
            className="h-auto min-h-7 w-full justify-between gap-1 text-xs font-normal"
          >
            <OverflowPills labels={selected.map(labelOf)} />
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
          </Button>
        </PopoverTrigger>
        <PopoverContent align="start" className="w-80 p-3">
          {checklist}
        </PopoverContent>
      </Popover>
    </div>
  );

  /** Until the values have arrived (or for a key no longer offered) the key itself stands in. */
  function labelOf(key: string): string {
    return options.find((it) => it.value === key)?.label ?? key;
  }
}
