"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { FilterElement } from "@/lib/rs/types";
import type { PeriodKindId } from "@/lib/date-period";
import { FilterFieldPicker } from "./filter-field-picker";
import { FilterPeriodKindsProvider } from "./filter-period-kinds";
import { FilterPill } from "./filter-pill";
import { withFilterValue, type FilterValues } from "./filter-value";
import {
  clearHistoryFilters,
  historyFilterActive,
  historyFilterGroupOf,
  mergeHistoryFilters,
  withoutHistoryFilters,
  HISTORY_FILTER_GROUP_ID,
} from "./history-filter";
import { HistoryFilterPill } from "./history-filter-pill";

interface FilterPillsProps {
  /** Field definitions, for labels and for resolving LIST values to their text. */
  elements: FilterElement[];
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  /**
   * The arts the period filters of this list offer, from the page's declaration. Absent leaves the
   * default (`Monat`, `Jahr bis heute`) — see [FilterPeriodKindsProvider].
   */
  periodKinds?: readonly PeriodKindId[];
  /** Sits at the end of the row — meant for the saved-filters menu. */
  trailing?: React.ReactNode;
  className?: string;
}

/**
 * The list's filters as pills: each one opens its own input, and the "add filter" chip
 * picks another field — or opens every field at once (see [FilterFieldPicker]).
 *
 * The three change-history fields share one pill under the pseudo id `historyFilter`; see
 * [historyFilterGroupOf].
 */
export function FilterPills({
  elements,
  values,
  onChange,
  periodKinds,
  trailing,
  className,
}: FilterPillsProps) {
  const t = useTranslations("filter");
  const [openId, setOpenId] = useState<string | null>(null);
  // A field picked from the chip: shown as an empty pill until it is saved or dropped.
  const [pendingId, setPendingId] = useState<string | null>(null);
  const activeCount = Object.keys(values).length;
  const history = historyFilterGroupOf(elements);
  const showHistory =
    history != null &&
    (historyFilterActive(values) || pendingId === HISTORY_FILTER_GROUP_ID);

  // Derived, and in backend order, so pills don't jump around as values come and go.
  const shown = withoutHistoryFilters(elements).filter(
    (element) =>
      element.defaultFilter || element.id in values || element.id === pendingId
  );

  function close() {
    setOpenId(null);
    setPendingId(null);
  }

  return (
    // Around the whole row, so the fields of the pills and the ones the "all filters" dialog repeats
    // offer the same arts.
    <FilterPeriodKindsProvider periodKinds={periodKinds}>
      <div className={cn("flex flex-wrap items-center gap-1.5", className)}>
        {showHistory && (
          <HistoryFilterPill
            group={history}
            values={values}
            open={openId === HISTORY_FILTER_GROUP_ID}
            onOpenChange={(open) =>
              open ? setOpenId(HISTORY_FILTER_GROUP_ID) : close()
            }
            onSave={(draft) => {
              onChange(mergeHistoryFilters(values, draft));
              close();
            }}
            onDelete={() => {
              onChange(clearHistoryFilters(values));
              close();
            }}
          />
        )}
        {shown.map((element) => (
          <FilterPill
            key={element.id}
            element={element}
            value={values[element.id]}
            open={openId === element.id}
            onOpenChange={(open) => (open ? setOpenId(element.id) : close())}
            removable={!element.defaultFilter}
            onSave={(value) => {
              onChange(withFilterValue(values, element.id, value));
              close();
            }}
            onDelete={() => {
              onChange(withFilterValue(values, element.id, undefined));
              close();
            }}
          />
        ))}
        <FilterFieldPicker
          entries={[
            ...(history
              ? [{ id: HISTORY_FILTER_GROUP_ID, label: t("history") }]
              : []),
            ...withoutHistoryFilters(elements).map((element) => ({
              id: element.id,
              label: element.label ?? element.id,
              tooltip: element.tooltip,
            })),
          ]}
          activeIds={[
            ...(showHistory ? [HISTORY_FILTER_GROUP_ID] : []),
            ...shown.map((element) => element.id),
          ]}
          onSelect={(id) => {
            // Keyed by id, so a pending pill mounts with its popover already open.
            if (!(id in values)) setPendingId(id);
            setOpenId(id);
          }}
          elements={elements}
          values={values}
          onApply={onChange}
        />
        {activeCount > 0 && (
          <button
            type="button"
            onClick={() => {
              close();
              onChange({});
            }}
            className="cursor-pointer px-1 text-xs font-medium text-muted-foreground hover:text-foreground"
          >
            {t("clearAll")}
          </button>
        )}
        {trailing}
      </div>
    </FilterPeriodKindsProvider>
  );
}
