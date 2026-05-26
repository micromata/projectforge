"use client";

import type { Column } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  SortingIcon,
} from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";

interface DataTableColumnHeaderProps<TData, TValue> {
  column: Column<TData, TValue>;
  children: React.ReactNode;
  className?: string;
}

export function DataTableColumnHeader<TData, TValue>({
  column,
  children,
  className,
}: DataTableColumnHeaderProps<TData, TValue>) {
  if (!column.getCanSort()) {
    return <span className={className}>{children}</span>;
  }

  const sorted = column.getIsSorted();

  return (
    <button
      type="button"
      onClick={() => column.toggleSorting(sorted === "asc")}
      className={cn(
        "group inline-flex select-none items-center gap-1 text-[11px] font-bold uppercase tracking-wider transition-colors",
        sorted ? "text-primary" : "text-muted-foreground hover:text-foreground",
        className
      )}
    >
      {children}
      {sorted === "asc" ? (
        <HugeiconsIcon icon={ArrowUp01Icon} size={12} />
      ) : sorted === "desc" ? (
        <HugeiconsIcon icon={ArrowDown01Icon} size={12} />
      ) : (
        <HugeiconsIcon
          icon={SortingIcon}
          size={12}
          className="opacity-0 transition-opacity group-hover:opacity-60"
        />
      )}
    </button>
  );
}
