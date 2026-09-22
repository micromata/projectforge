"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import { LookupLoadingRow } from "@/components/shared/lookup-loading-row";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { isNearBottom, useEntityLookup } from "@/hooks/use-entity-lookup";
import type { FilterElement } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { TextField, type FilterInputProps } from "./filter-field-inputs";

/** The number segment of a suggestion's display name ("6.300.00.00: Urlaub – …" → "6.300.00.00"). */
function numberOf(displayName: string): string {
  return displayName.split(":")[0]?.trim() ?? displayName;
}

/**
 * The cost unit (Kost2) list filter (AutoCompletion.Type.KOST2, see Kost2FilterUtils): a text input
 * whose free text filters the list directly — a number-looking term as a prefix on the cost number
 * (trimming trailing segments widens it: 6.300.00.00 → 6.300.00 → 6.300 → 6), any other text across the
 * cost unit's number, description and project name.
 *
 * While typing, a type-ahead against `cost2/autosearch` suggests concrete cost units — searchable by
 * number, description, project *and* customer name. Picking one inserts **only its number**, which stays
 * editable, so the user can trim it to broaden the search. The value is stored raw (no LIKE wildcards):
 * the backend adds the prefix `*` and chooses the fields.
 */
export function FilterKost2Field({
  element,
  value,
  onChange,
  label,
  id,
  autoFocus,
  onSubmit,
}: FilterInputProps & { element: FilterElement }) {
  const t = useTranslations();
  const url = element.autoCompletion?.url;
  const [open, setOpen] = useState(false);

  // The input's text *is* the filter value — not a separate search term. Stored raw under `value.value`.
  const text = value?.value ?? "";
  const setText = (next: string) =>
    onChange(next.trim() === "" ? undefined : { value: next });

  const { entries, isFetching, isLoadingMore, loadMore } =
    useEntityLookup<EntityRef>({
      url,
      search: text,
      open: open && url != null,
      minChars: element.autoCompletion?.minChars,
    });

  // No lookup url: nothing to suggest, but the free text still filters — a plain (raw) text input.
  if (!url) {
    return (
      <TextField
        value={value}
        onChange={onChange}
        label={label}
        id={id}
        autoFocus={autoFocus}
        onSubmit={onSubmit}
        raw
      />
    );
  }

  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverAnchor asChild>
          <Input
            id={`filter-${id}`}
            autoFocus={autoFocus}
            aria-label={label}
            value={text}
            onFocus={() => setOpen(true)}
            onChange={(e) => {
              setText(e.target.value);
              setOpen(true);
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                setOpen(false);
                onSubmit?.();
              } else if (e.key === "Escape") {
                setOpen(false);
              }
            }}
            className="h-8 text-xs"
          />
        </PopoverAnchor>
        <PopoverContent
          align="start"
          // Keep the caret in the input, so the popover suggests *while* the user keeps typing.
          onOpenAutoFocus={(e) => e.preventDefault()}
          onCloseAutoFocus={(e) => e.preventDefault()}
          className="w-(--radix-popover-trigger-width) min-w-56 p-0"
        >
          <div
            className="max-h-64 overflow-y-auto py-1"
            onScroll={(event) => {
              if (isNearBottom(event.currentTarget)) loadMore();
            }}
          >
            {entries.length === 0 ? (
              <div className="px-2 py-1.5 text-center text-xs text-muted-foreground">
                {isFetching ? t("loading") : t("nothingFound")}
              </div>
            ) : (
              entries.map((entry) => (
                <button
                  key={entry.id}
                  type="button"
                  // On mouse down, not click: it keeps the caret in the input (no blur), so the field
                  // stays focused and editable after the number is inserted.
                  onMouseDown={(e) => {
                    e.preventDefault();
                    setText(numberOf(entry.displayName));
                    setOpen(false);
                  }}
                  className={cn(
                    "block w-full cursor-pointer px-2 py-1.5 text-left text-xs",
                    "hover:bg-accent hover:text-accent-foreground"
                  )}
                >
                  {entry.displayName}
                </button>
              ))
            )}
            {isLoadingMore && <LookupLoadingRow />}
          </div>
        </PopoverContent>
      </Popover>
    </div>
  );
}
