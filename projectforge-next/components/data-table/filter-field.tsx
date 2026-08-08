"use client";

import type { FilterElement, MagicFilterEntryValue } from "@/lib/rs/types";
import {
  BooleanField,
  ListField,
  RangeField,
  TextField,
} from "./filter-field-inputs";

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
 * OBJECT fields (entity lookup via autoCompletion) fall back to a plain text
 * input for now — a proper autocomplete needs its own component.
 */
export function FilterField({ element, ...rest }: FilterFieldProps) {
  const props = { ...rest, label: element.label ?? element.id, id: element.id };

  switch (element.filterType) {
    case "LIST":
      return <ListField element={element} {...props} />;
    case "BOOLEAN":
      return <BooleanField {...props} />;
    case "DATE":
    case "TIMESTAMP":
      return <RangeField {...props} />;
    default:
      return <TextField {...props} />;
  }
}
