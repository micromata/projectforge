"use client";

import type { MagicFilterEntryValue } from "@/lib/rs/types";
import { FilterObjectField } from "./filter-object-field";
import { TextField } from "./filter-field-inputs";
import { TimestampRangeField } from "./filter-timestamp-field";
import { withFilterValue, type FilterValues } from "./filter-value";
import {
  HISTORY_FILTER_FIELDS,
  type HistoryFilterGroup,
} from "./history-filter";

interface HistoryFilterFieldsProps {
  group: HistoryFilterGroup;
  /** Only the three history keys; see [pickHistoryFilters]. */
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  autoFocus?: boolean;
  /** Enter in one of the inputs; carries the values that are in effect afterwards. */
  onSubmit?: (values: FilterValues) => void;
  /**
   * `grid` hands the three criteria to the caller's grid as its own cells instead of stacking them
   * in one. For the dialog, whose section is three columns wide: stacked, the three sat in a single
   * column and left two thirds of the row empty.
   */
  layout?: "stacked" | "grid";
}

/**
 * The three history criteria: who changed it, when, and which value the change history holds.
 * Rendered identically by the group pill and by the "all filters" dialog, so the two never drift
 * apart — only their arrangement differs (`layout`).
 *
 * The labels are the layout's own (`element.label`), not looked up here — they are the same texts
 * Wicket shows.
 */
export function HistoryFilterFields({
  group,
  values,
  onChange,
  autoFocus,
  onSubmit,
  layout = "stacked",
}: HistoryFilterFieldsProps) {
  return (
    // `contents`: the wrapper leaves no box of its own, so each field becomes a cell of the grid
    // around it.
    <div className={layout === "grid" ? "contents" : "space-y-2"}>
      {group.user && (
        <FilterObjectField
          element={group.user}
          label={group.user.label ?? group.user.id}
          id={group.user.id}
          autoFocus={autoFocus}
          value={values[HISTORY_FILTER_FIELDS.user]}
          onChange={(value) => change(HISTORY_FILTER_FIELDS.user, value)}
          onSubmit={(value) => submit(HISTORY_FILTER_FIELDS.user, value)}
        />
      )}
      {group.interval && (
        // Two columns of the three: the widest of the criteria, and only in a column that wide do
        // its two bounds fit next to each other rather than under one another.
        <div className={layout === "grid" ? "lg:col-span-2" : undefined}>
          <TimestampRangeField
            element={group.interval}
            label={group.interval.label ?? group.interval.id}
            id={group.interval.id}
            // Only the first field takes the focus; without a user field that is this one.
            autoFocus={autoFocus && !group.user}
            value={values[HISTORY_FILTER_FIELDS.interval]}
            onChange={(value) => change(HISTORY_FILTER_FIELDS.interval, value)}
            onSubmit={(value) => submit(HISTORY_FILTER_FIELDS.interval, value)}
          />
        </div>
      )}
      {group.value && (
        <TextField
          // The backend's full-text query adds its own wildcards.
          raw
          label={group.value.label ?? group.value.id}
          id={group.value.id}
          autoFocus={autoFocus && !group.user && !group.interval}
          value={values[HISTORY_FILTER_FIELDS.value]}
          onChange={(value) => change(HISTORY_FILTER_FIELDS.value, value)}
          onSubmit={(value) => submit(HISTORY_FILTER_FIELDS.value, value)}
        />
      )}
    </div>
  );

  function change(field: string, value: MagicFilterEntryValue | undefined) {
    onChange(withFilterValue(values, field, value));
  }

  /**
   * A field that submits on Enter may pass the value it just changed to, because its `onChange` has
   * not come back through `values` yet at that point (see FilterInputProps.onSubmit).
   */
  function submit(field: string, value?: MagicFilterEntryValue | undefined) {
    onSubmit?.(
      value === undefined ? values : withFilterValue(values, field, value)
    );
  }
}
