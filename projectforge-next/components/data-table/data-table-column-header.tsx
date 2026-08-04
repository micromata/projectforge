"use client";

import type { Column } from "@tanstack/react-table";
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
  children: React.ReactNode;
  className?: string;
  /** Filter input to offer; omit to disable filtering for this column. */
  filterKind?: FilterKind;
}

export function DataTableColumnHeader<TData, TValue>({
  column,
  children,
  className,
  filterKind,
}: DataTableColumnHeaderProps<TData, TValue>) {
  const t = useTranslations("table");
  const canFilter = !!filterKind && column.getCanFilter();

  if (!column.getCanSort() && !canFilter) {
    return <span className={className}>{children}</span>;
  }

  const sorted = column.getIsSorted();

  return (
    <span className="inline-flex items-center gap-0.5">
      {column.getCanSort() ? (
        <button
          type="button"
          onClick={() => column.toggleSorting(sorted === "asc")}
          className={cn(
            "group inline-flex min-w-0 select-none items-center gap-1 text-[11px] font-bold uppercase tracking-wider transition-colors",
            sorted
              ? "text-primary"
              : "text-muted-foreground hover:text-foreground",
            className
          )}
        >
          <span className="truncate">{children}</span>
          {sorted === "asc" ? (
            <HugeiconsIcon icon={ArrowUp01Icon} size={12} className="shrink-0" />
          ) : sorted === "desc" ? (
            <HugeiconsIcon
              icon={ArrowDown01Icon}
              size={12}
              className="shrink-0"
            />
          ) : (
            <HugeiconsIcon
              icon={SortingIcon}
              size={12}
              className="shrink-0 opacity-0 transition-opacity group-hover:opacity-60"
            />
          )}
        </button>
      ) : (
        <span className={cn("truncate", className)}>{children}</span>
      )}

      {canFilter && (
        <Popover>
          <PopoverTrigger asChild>
            <button
              type="button"
              aria-label={t("filter")}
              className={cn(
                "shrink-0 rounded-sm p-0.5 transition-opacity",
                column.getIsFiltered()
                  ? "text-primary opacity-100"
                  : "text-muted-foreground opacity-0 hover:opacity-70 focus-visible:opacity-100 group-hover/th:opacity-60"
              )}
            >
              <HugeiconsIcon icon={FilterIcon} size={11} />
            </button>
          </PopoverTrigger>
          <PopoverContent align="start" className="w-auto p-0">
            <ColumnFilter column={column} kind={filterKind} />
          </PopoverContent>
        </Popover>
      )}
    </span>
  );
}
