"use client";

import type { Column, Table } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  FilterIcon,
  SortingIcon,
} from "@hugeicons/core-free-icons";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { ColumnFilter, type FilterKind } from "./column-filter";

interface DataTableColumnHeaderProps<TData, TValue> {
  column: Column<TData, TValue>;
  /** From the header context; needed to tell whether several columns are sorted. */
  table: Table<TData>;
  children: React.ReactNode;
  className?: string;
  /** Filter input to offer; omit to disable filtering for this column. */
  filterKind?: FilterKind;
}

/**
 * Column header content. Sorting is handled by a click on the whole header cell
 * (see DataTable), not by a button in here — a button would compete with the
 * filter icon for space and push it out of a narrow column.
 */
export function DataTableColumnHeader<TData, TValue>({
  column,
  table,
  children,
  className,
  filterKind,
}: DataTableColumnHeaderProps<TData, TValue>) {
  const t = useTranslations("columns");
  const tFilter = useTranslations("filter");
  const canFilter = !!filterKind && column.getCanFilter();
  const sorted = column.getIsSorted();
  // A "1" is just noise while a single column is sorted.
  const sortIndex =
    sorted && table.getState().sorting.length > 1
      ? column.getSortIndex() + 1
      : null;

  return (
    <span className="flex items-center gap-1">
      {/* min-w-0 lets the label shrink below its content width; without it the
          filter icon gets pushed out of the cell and clipped. */}
      <span className={cn("min-w-0 flex-1 truncate", className)}>
        {children}
      </span>

      {/* shrink-0 on the indicators: they keep their width in narrow columns. */}
      {sorted && (
        <span
          className="flex shrink-0 items-center text-primary"
          title={sorted === "asc" ? t("sortAscending") : t("sortDescending")}
        >
          <HugeiconsIcon
            icon={sorted === "asc" ? ArrowUp01Icon : ArrowDown01Icon}
            size={12}
          />
          {sortIndex !== null && (
            <span
              className="text-[9px] font-bold tabular-nums"
              title={t("sortPosition", { arg0: sortIndex })}
            >
              {sortIndex}
            </span>
          )}
        </span>
      )}
      {!sorted && column.getCanSort() && (
        <HugeiconsIcon
          icon={SortingIcon}
          size={12}
          className="shrink-0 text-muted-foreground opacity-0 transition-opacity group-hover/th:opacity-60"
        />
      )}

      {canFilter && (
        <Popover>
          <PopoverTrigger asChild>
            <button
              type="button"
              aria-label={tFilter("title")}
              // Stop the click from reaching the header cell, which sorts.
              onClick={(e) => e.stopPropagation()}
              className={cn(
                "shrink-0 rounded-sm p-0.5 transition-colors",
                // Always visible: hidden-until-hover is unreachable on touch
                // devices and in columns narrowed down to their minimum.
                column.getIsFiltered()
                  ? "bg-primary/15 text-primary"
                  : "text-muted-foreground/50 hover:text-foreground"
              )}
            >
              <HugeiconsIcon icon={FilterIcon} size={11} />
            </button>
          </PopoverTrigger>
          <PopoverContent
            align="start"
            className="w-auto p-0"
            // The trigger lives inside a sortable header cell.
            onClick={(e) => e.stopPropagation()}
          >
            <ColumnFilter column={column} kind={filterKind} />
          </PopoverContent>
        </Popover>
      )}
    </span>
  );
}
