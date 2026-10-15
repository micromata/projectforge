"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  Cancel01Icon,
  Tick02Icon,
} from "@hugeicons/core-free-icons";
import type { DynamicComponentProps } from "../../dynamic-renderer";
import { useDynamicLayout } from "../../dynamic-context";
import { DynamicField } from "../dynamic-field";
import {
  selectSpecOf,
  selectedOptions,
  toDataValue,
  toOption,
  type SelectOption,
} from "./select-values";
import { LookupLoadingRow } from "@/components/shared/lookup-loading-row";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { isNearBottom, useEntityLookup } from "@/hooks/use-entity-lookup";
import { getByPath, type DataObject } from "@/lib/dynamic/path";
import { cn } from "@/lib/utils";

/**
 * SELECT and CREATABLE_SELECT (org.projectforge.ui.UISelect).
 *
 * One component covers all four flavours the protocol allows, because they differ only in flags:
 * single or multi (`multi`), a fixed list or a server lookup (`autoCompletion.url`), and ids or
 * whole entities in the data (`autoCompletion.type`). CREATABLE_SELECT additionally accepts a
 * value the user typed but that no option offers.
 */
export function DynamicSelect({ node }: DynamicComponentProps) {
  const { data, setData } = useDynamicLayout();
  // These labels have no backend counterpart - the layout's translations only cover its own fields.
  const t = useTranslations("select");
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");

  const spec = selectSpecOf(node);
  const creatable = node.type === "CREATABLE_SELECT";
  const selected = selectedOptions(data, spec);
  const lookupUrl = spec.autoCompletion?.url;
  const minChars = spec.autoCompletion?.minChars ?? 2;

  // The lookup may need other fields of the form as context (e.g. the selected project).
  const params = Object.fromEntries(
    Object.entries(spec.autoCompletion?.urlParams ?? {}).map(
      ([param, path]) => [param, getByPath(data, path)]
    )
  );

  // Opening offers the first entries without a term; scrolling the list asks for more (see
  // useEntityLookup).
  const {
    entries: fetched,
    isLoadingMore,
    loadMore,
  } = useEntityLookup<DataObject>({
    url: lookupUrl,
    search,
    params,
    open,
    minChars,
  });

  const remote = fetched
    .map((entry) => toOption(entry, spec.valueProperty, spec.labelProperty))
    .filter((option): option is SelectOption => option != null);
  // The offered values come from the layout; a lookup adds to them rather than replacing them,
  // so the current value stays selectable after a search.
  const options = mergeOptions(spec.options, remote, selected);

  function commit(next: SelectOption[]) {
    setData({ [spec.id]: toDataValue(next, spec) });
  }

  function toggle(option: SelectOption) {
    if (!spec.multi) {
      commit([option]);
      setOpen(false);
      return;
    }
    const isSelected = selected.some((it) => it.value === option.value);
    commit(
      isSelected
        ? selected.filter((it) => it.value !== option.value)
        : [...selected, option]
    );
  }

  return (
    <DynamicField node={node}>
      {(domId, hasError) => (
        <Popover open={open} onOpenChange={setOpen}>
          <PopoverTrigger asChild>
            <Button
              id={domId}
              variant="outline"
              role="combobox"
              className={cn(
                "h-auto min-h-7 w-full justify-between font-normal",
                hasError && "border-destructive"
              )}
            >
              <span className="flex flex-wrap items-center gap-1">
                {selected.length === 0 && (
                  <span className="text-muted-foreground">{t("empty")}</span>
                )}
                {spec.multi
                  ? selected.map((option) => (
                      <Badge key={option.value} variant="secondary">
                        {option.label}
                        <span
                          role="button"
                          tabIndex={-1}
                          aria-label={t("remove", { label: option.label })}
                          onClick={(e) => {
                            e.stopPropagation();
                            commit(
                              selected.filter((it) => it.value !== option.value)
                            );
                          }}
                        >
                          <HugeiconsIcon icon={Cancel01Icon} />
                        </span>
                      </Badge>
                    ))
                  : selected[0]?.label}
              </span>
              <HugeiconsIcon icon={ArrowDown01Icon} />
            </Button>
          </PopoverTrigger>
          <PopoverContent
            align="start"
            className="w-(--radix-popover-trigger-width) p-0"
          >
            <Command
              // The options are filtered by the backend when there is a lookup url.
              shouldFilter={lookupUrl == null}
            >
              <CommandInput
                value={search}
                onValueChange={setSearch}
                placeholder={t("search")}
              />
              {/* cmdk's list is its own scroll container, so a lookup's next page is asked for from
                  here; the arrow keys scroll it too, which pages for the keyboard as well. */}
              <CommandList
                onScroll={(event) => {
                  if (isNearBottom(event.currentTarget)) loadMore();
                }}
              >
                <CommandEmpty>{t("noOptions")}</CommandEmpty>
                {creatable &&
                  search.length > 0 &&
                  !options.some((it) => it.label === search) && (
                    <CommandItem
                      value={search}
                      onSelect={() => {
                        toggle({ value: search, label: search });
                        setSearch("");
                      }}
                    >
                      {t("create", { value: search })}
                    </CommandItem>
                  )}
                {options.map((option) => (
                  <CommandItem
                    key={option.value}
                    value={option.label}
                    onSelect={() => toggle(option)}
                  >
                    {selected.some((it) => it.value === option.value) && (
                      <HugeiconsIcon icon={Tick02Icon} />
                    )}
                    {option.label}
                  </CommandItem>
                ))}
                {isLoadingMore && <LookupLoadingRow />}
              </CommandList>
            </Command>
          </PopoverContent>
        </Popover>
      )}
    </DynamicField>
  );
}

function mergeOptions(...lists: SelectOption[][]): SelectOption[] {
  const byValue = new Map<string, SelectOption>();
  lists.flat().forEach((option) => {
    if (!byValue.has(option.value)) byValue.set(option.value, option);
  });
  return [...byValue.values()];
}
