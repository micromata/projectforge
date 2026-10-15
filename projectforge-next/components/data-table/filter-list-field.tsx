"use client";

import { Label } from "@/components/ui/label";
import { ValueCombobox } from "@/components/shared/value-combobox";
import { ValueOptionList } from "@/components/shared/value-option-list";
import type { FilterElement } from "@/lib/rs/types";
import type { FilterInputProps } from "./filter-field-inputs";

/**
 * A LIST filter (org.projectforge.ui.filter.UIFilterListElement): picks among the values the
 * backend offers, e.g. the states of an order.
 *
 * In the "all filters" dialog a combobox, so the field stays one line tall in the grid and a long
 * list of states can be searched instead of scrolled. In a pill popover (`inline`) the same list
 * lies open instead: a popover of its own would open over the pill's own save button, which is the
 * one thing the user has to reach afterwards.
 */
export function ListField({
  element,
  value,
  onChange,
  label,
  id,
  autoFocus,
  inline,
}: FilterInputProps & { element: FilterElement; inline?: boolean }) {
  const options =
    element.values?.map((option) => ({
      value: option.id,
      label: option.displayName,
    })) ?? [];
  // An emptied field is dropped from the filter, as everywhere else.
  const onValues = (values: string[]) =>
    onChange(values.length ? { values } : undefined);

  if (inline) {
    return (
      <div className="space-y-1">
        <p className="text-xs">{label}</p>
        <ValueOptionList
          aria-label={label}
          className="rounded-md border"
          options={options}
          multi={element.multi}
          selected={value?.values ?? []}
          onChange={onValues}
          autoFocus={autoFocus}
        />
      </div>
    );
  }

  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <ValueCombobox
        id={`filter-${id}`}
        aria-label={label}
        multi={element.multi}
        options={options}
        selected={value?.values ?? []}
        onChange={onValues}
      />
    </div>
  );
}
