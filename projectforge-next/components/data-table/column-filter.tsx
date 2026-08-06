"use client";

import { useDeferredValue, useState } from "react";
import type { Column } from "@tanstack/react-table";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import type {
  ColumnFilterValue,
  FilterKind,
  FilterMode,
} from "./column-filter-types";
import { ComparisonFilter } from "./comparison-filter";
import { SelectionFilter } from "./selection-filter";
import {
  SELECTION_PREFERRED_MAX,
  useDistinctValueCount,
} from "./use-distinct-filter-values";

interface ColumnFilterProps<TData> {
  column: Column<TData, unknown>;
  kind: FilterKind;
}

function initialMode(
  value: ColumnFilterValue | undefined,
  kind: FilterKind,
  preferSelection: boolean
): FilterMode {
  // An existing filter wins: whoever set it wants to see it, however wide the column.
  if (Array.isArray(value)) return "selection";
  if (value?.type) return value.type;
  return kind === "text" && preferSelection ? "selection" : kind;
}

export function ColumnFilter<TData>({
  column,
  kind,
}: ColumnFilterProps<TData>) {
  const t = useTranslations("filter");
  const tCommon = useTranslations();
  const formatCtx = useFormatContext();
  const current = column.getFilterValue() as ColumnFilterValue | undefined;
  const valueCount = useDistinctValueCount(column);
  const [mode, setMode] = useState<FilterMode>(() =>
    initialMode(current, kind, valueCount <= SELECTION_PREFERRED_MAX)
  );

  // A long selection list costs hundreds of checkbox mounts. Rendering it from a
  // deferred value lets React paint the spinner first, so switching modes stays
  // responsive instead of freezing on the click. Short lists switch straight
  // through — deferring them would only flash the spinner for a frame.
  const deferredMode = useDeferredValue(mode);
  const isSlow = valueCount > SELECTION_PREFERRED_MAX;
  const shownMode = isSlow ? deferredMode : mode;
  const isBuilding = shownMode !== mode;

  return (
    <div className="w-64 p-2">
      <Select value={mode} onValueChange={(v) => setMode(v as FilterMode)}>
        <SelectTrigger size="sm" className="mb-2 w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {/* With the count: it says up front how long the list will be and
              whether it is worth opening at all. */}
          <SelectItem value="selection">
            {`${t("selection")} (${formatNumber(valueCount, formatCtx)})`}
          </SelectItem>
          {kind === "text" && <SelectItem value="text">{t("text")}</SelectItem>}
          {kind === "number" && (
            <SelectItem value="number">{t("number")}</SelectItem>
          )}
          {kind === "date" && <SelectItem value="date">{t("date")}</SelectItem>}
        </SelectContent>
      </Select>

      {isBuilding ? (
        <div
          aria-busy
          aria-live="polite"
          className="flex items-center justify-center gap-2 py-6 text-xs text-muted-foreground"
        >
          <Spinner className="h-4 w-4 border-2" />
          {tCommon("loading")}
        </div>
      ) : shownMode === "selection" ? (
        <SelectionFilter column={column} />
      ) : (
        <ComparisonFilter column={column} mode={shownMode} />
      )}

      {column.getIsFiltered() && (
        <Button
          variant="ghost"
          size="sm"
          className="mt-2 w-full text-xs"
          onClick={() => column.setFilterValue(undefined)}
        >
          {t("reset")}
        </Button>
      )}
    </div>
  );
}
