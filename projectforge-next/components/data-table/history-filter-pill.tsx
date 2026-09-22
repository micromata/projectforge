"use client";

import { useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import type { FilterValues } from "./filter-value";
import { FilterPillShell } from "./filter-pill-shell";
import { useDebouncedApply } from "./use-debounced-apply";
import {
  historyFilterActive,
  pickHistoryFilters,
  type HistoryFilterGroup,
} from "./history-filter";
import { HistoryFilterFields } from "./history-filter-fields";
import { describeHistoryFilter } from "./history-filter-summary";

interface HistoryFilterPillProps {
  group: HistoryFilterGroup;
  /** All filter values; the pill reads and writes only the three history keys. */
  values: FilterValues;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Applies the three history values to the list without closing — the list refetches live. */
  onSave: (values: FilterValues) => void;
  onDelete: () => void;
}

/**
 * The change history as one pill: "Modified: Kai Reinhard, 15.07.2026, 10:30 – …, Titel".
 *
 * Three backend fields, one question — so they share a pill, apply together and are removed
 * together, the way Wicket's fieldset lets you pick a user and/or a period at once. The wire format
 * stays three separate entries. Like the single-field pill, edits apply to the list live and
 * "Abbrechen" restores the values the popover opened with.
 */
export function HistoryFilterPill({
  group,
  values,
  open,
  onOpenChange,
  onSave,
  onDelete,
}: HistoryFilterPillProps) {
  const t = useTranslations("filter");
  const ctx = useFormatContext();
  const [draft, setDraft] = useState(() => pickHistoryFilters(values));
  // The history values the popover opened with, restored by "Abbrechen".
  const baseline = useRef(draft);

  useDebouncedApply(draft, pickHistoryFilters(values), onSave);

  return (
    <FilterPillShell
      label={t("history")}
      text={describeHistoryFilter(values, ctx)}
      active={historyFilterActive(values)}
      open={open}
      onOpenChange={(next) => {
        // Re-seed on open so a live-applied edit can still be taken back.
        if (next) {
          const picked = pickHistoryFilters(values);
          setDraft(picked);
          baseline.current = picked;
        }
        onOpenChange(next);
      }}
      // Always removable: the group is not one of the backend's default filters, and emptying
      // three fields one by one would be the only alternative.
      removable
      onCancel={cancel}
      onDelete={onDelete}
      // Roomier than a single-field pill: it holds an autocomplete, two bounds and the presets.
      contentClassName="w-80"
    >
      <HistoryFilterFields
        group={group}
        values={draft}
        onChange={setDraft}
        autoFocus
        // Enter is "done": apply straight away, without waiting for the debounce, and close.
        onSubmit={(next) => {
          onSave(next);
          onOpenChange(false);
        }}
      />
    </FilterPillShell>
  );

  /** Restore the history values the popover opened with and close. */
  function cancel() {
    setDraft(baseline.current);
    onSave(baseline.current);
    onOpenChange(false);
  }
}
