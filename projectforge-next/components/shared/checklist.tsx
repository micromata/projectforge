"use client";

import { useId, useMemo, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { ChecklistRows } from "./checklist-rows";
import {
  matchesTerm,
  visibleSelectionState,
  withVisible,
} from "./checklist-selection";

export interface ChecklistOption {
  value: string;
  label: string;
  /** Marked as typed text rather than a picked record (see [FreeTextBadge]). */
  freeText?: boolean;
}

export interface ChecklistProps {
  options: ChecklistOption[];
  /** The ticked options by value. */
  selected: string[];
  onChange: (values: string[]) => void;
  loading?: boolean;
  autoFocus?: boolean;
  "aria-label"?: string;
  className?: string;
}

/**
 * Picks any number of entries from a long list, as Excel's autofilter does: a search narrows the list,
 * each entry is ticked on and off, and "select all" acts on the entries the search shows.
 *
 * Unlike [ValueOptionList] meant for hundreds of entries (the customers of the order book), where
 * ticking the matches of a search at once beats picking them one by one.
 */
export function Checklist({
  options,
  selected,
  onChange,
  loading,
  autoFocus,
  "aria-label": ariaLabel,
  className,
}: ChecklistProps) {
  const t = useTranslations();
  const [term, setTerm] = useState("");
  const id = useId();
  const matches = useMemo(
    () => options.filter((option) => matchesTerm(option.label, term)),
    [options, term]
  );
  const visible = matches.map((option) => option.value);
  const allState = visibleSelectionState(selected, visible);

  return (
    <div
      className={cn("flex flex-col gap-2", className)}
      role="group"
      aria-label={ariaLabel}
    >
      <Input
        type="search"
        value={term}
        onChange={(event) => setTerm(event.target.value)}
        placeholder={t("select.search")}
        aria-label={t("select.search")}
        autoFocus={autoFocus}
        className="h-8"
      />
      {loading ? (
        <p className="px-1 text-xs text-muted-foreground">{t("loading")}</p>
      ) : matches.length === 0 ? (
        <p className="px-1 text-xs text-muted-foreground">
          {t("select.noOptions")}
        </p>
      ) : (
        <div className="rounded-md border">
          <label className="flex cursor-pointer items-center gap-2 border-b px-2 py-1.5 text-sm font-medium">
            <Checkbox
              checked={allState}
              onCheckedChange={() =>
                onChange(withVisible(selected, visible, allState !== true))
              }
            />
            {t("selectAll")}
          </label>
          <ChecklistRows
            options={matches}
            selected={selected}
            onChange={onChange}
            term={term}
            describedBy={`${id}-count`}
          />
        </div>
      )}
      <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
        <span id={`${id}-count`}>
          {t("select.checklist.selectedOf", {
            arg0: selected.length,
            arg1: options.length,
          })}
        </span>
        {selected.length > 0 && (
          <Button
            type="button"
            variant="link"
            size="sm"
            className="h-auto p-0 text-xs"
            onClick={() => onChange([])}
          >
            {t("filter.clearAll")}
          </Button>
        )}
      </div>
    </div>
  );
}
