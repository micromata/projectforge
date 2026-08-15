"use client";

import { Label } from "@/components/ui/label";
import { ValueCombobox } from "@/components/shared/value-combobox";
import type { FilterElement } from "@/lib/rs/types";
import type { FilterInputProps } from "./filter-field-inputs";

/**
 * A LIST filter (org.projectforge.ui.filter.UIFilterListElement): picks among the values the
 * backend offers, e.g. the states of an order.
 *
 * A combobox rather than a checkbox list, so the field stays one line tall in the dialog's grid
 * and a long list of states can be searched instead of scrolled.
 */
export function ListField({
  element,
  value,
  onChange,
  label,
  id,
}: FilterInputProps & { element: FilterElement }) {
  return (
    <div className="space-y-1">
      <Label htmlFor={`filter-${id}`} className="text-xs">
        {label}
      </Label>
      <ValueCombobox
        id={`filter-${id}`}
        aria-label={label}
        multi={element.multi}
        options={
          element.values?.map((option) => ({
            value: option.id,
            label: option.displayName,
          })) ?? []
        }
        selected={value?.values ?? []}
        // An emptied field is dropped from the filter, as everywhere else.
        onChange={(values) => onChange(values.length ? { values } : undefined)}
      />
    </div>
  );
}
