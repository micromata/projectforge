"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useFormatContext } from "@/hooks/use-format";
import type { FilterValues } from "./filter-value";
import { FilterPillShell } from "./filter-pill-shell";
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
  /** The three history values to keep, or `{}` to drop them all. */
  onSave: (values: FilterValues) => void;
  onDelete: () => void;
}

/**
 * The change history as one pill: "Modified: Kai Reinhard, 15.07.2026, 10:30 – …, Titel".
 *
 * Three backend fields, one question — so they share a pill, are saved together and are removed
 * together, the way Wicket's fieldset lets you pick a user and/or a period at once. The wire format
 * stays three separate entries.
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

  return (
    <FilterPillShell
      label={t("history")}
      text={describeHistoryFilter(values, ctx)}
      active={historyFilterActive(values)}
      open={open}
      onOpenChange={(next) => {
        // Re-seed on open so an abandoned edit doesn't come back.
        if (next) setDraft(pickHistoryFilters(values));
        onOpenChange(next);
      }}
      // Always removable: the group is not one of the backend's default filters, and emptying
      // three fields one by one would be the only alternative.
      removable
      onSave={() => onSave(draft)}
      onDelete={onDelete}
      // Roomier than a single-field pill: it holds an autocomplete, two bounds and the presets.
      contentClassName="w-80"
    >
      <HistoryFilterFields
        group={group}
        values={draft}
        onChange={setDraft}
        autoFocus
        onSubmit={onSave}
      />
    </FilterPillShell>
  );
}
