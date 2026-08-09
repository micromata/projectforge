"use client";

import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import {
  BooleanField,
  ListField,
  RangeField,
  TextField,
} from "./filter-field-inputs";
import { FilterObjectField } from "./filter-object-field";
import { TimestampRangeField } from "./filter-timestamp-field";

interface FilterFieldProps {
  element: FilterElement;
  value: MagicFilterEntryValue | undefined;
  onChange: (value: MagicFilterEntryValue | undefined) => void;
  /** Focus on mount, so a filter opened from the pill row is ready to type into. */
  autoFocus?: boolean;
  /** Enter in a single-line input; used by the pill popover to save and close. */
  onSubmit?: (value?: MagicFilterEntryValue | undefined) => void;
}

/**
 * One input per backend filter field, chosen by its filterType. Shared by the
 * pill popovers and the "all filters" dialog.
 *
 * DATE and TIMESTAMP are deliberately different components: a DATE compares two `yyyy-MM-dd`
 * bounds, while a TIMESTAMP needs a time of day on each — sent without one, the backend parses it
 * to null and drops the bound (see [TimestampRangeField]).
 */
export function FilterField({ element, ...rest }: FilterFieldProps) {
  const props = { ...rest, label: element.label ?? element.id, id: element.id };

  switch (element.filterType) {
    case "LIST":
      return <ListField element={element} {...props} />;
    case "BOOLEAN":
      return <BooleanField {...props} />;
    case "OBJECT":
      return <FilterObjectField element={element} {...props} />;
    case "TIMESTAMP":
      return <TimestampRangeField element={element} {...props} />;
    case "DATE":
      return <RangeField {...props} />;
    default:
      return <TextField {...props} />;
  }
}
