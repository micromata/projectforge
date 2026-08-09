"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { FilterElement } from "@/lib/rs/types";
import { FilterField } from "./filter-field";
import { withFilterValue, type FilterValues } from "./filter-value";
import {
  historyFilterGroupOf,
  mergeHistoryFilters,
  pickHistoryFilters,
  withoutHistoryFilters,
} from "./history-filter";
import { HistoryFilterFields } from "./history-filter-fields";

interface FilterFieldGridProps {
  elements: FilterElement[];
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  className?: string;
}

/**
 * Every filter field the backend offers, in as many columns as the width allows. The three
 * change-history fields share one cell, so the dialog groups them exactly as the pill row does.
 */
export function FilterFieldGrid({
  elements,
  values,
  onChange,
  className,
}: FilterFieldGridProps) {
  const t = useTranslations("filter");
  const history = historyFilterGroupOf(elements);
  const rest = withoutHistoryFilters(elements);

  if (elements.length === 0) {
    return <p className="text-xs text-muted-foreground">{t("noFields")}</p>;
  }

  return (
    <div
      className={cn(
        "grid gap-x-6 gap-y-3 sm:grid-cols-2 lg:grid-cols-3",
        className
      )}
    >
      {history && (
        // First, and its own column: the group is three inputs tall, and it is the one filter
        // every entity has in common.
        <div className="space-y-1.5">
          <p className="text-xs font-semibold">{t("history")}</p>
          <HistoryFilterFields
            group={history}
            values={pickHistoryFilters(values)}
            onChange={(draft) => onChange(mergeHistoryFilters(values, draft))}
          />
        </div>
      )}
      {rest.map((element) => (
        <div key={element.id} title={element.tooltip}>
          <FilterField
            element={element}
            value={values[element.id]}
            onChange={(value) =>
              onChange(withFilterValue(values, element.id, value))
            }
          />
        </div>
      ))}
    </div>
  );
}
