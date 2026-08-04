"use client";

import type { Table } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { PinIcon, TableIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

interface DataTableColumnPanelProps<TData> {
  table: Table<TData>;
  onReset?: () => void;
}

/** Column header text, falling back to the column id. */
function columnLabel<TData>(
  table: Table<TData>,
  columnId: string
): string {
  const header = table.getColumn(columnId)?.columnDef.header;
  return typeof header === "string" ? header : columnId;
}

/** Lets the user show/hide and pin columns. */
export function DataTableColumnPanel<TData>({
  table,
  onReset,
}: DataTableColumnPanelProps<TData>) {
  const t = useTranslations("columns");
  const columns = table.getAllLeafColumns().filter((c) => c.getCanHide());
  const visibleCount = columns.filter((c) => c.getIsVisible()).length;

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="gap-1.5">
          <HugeiconsIcon icon={TableIcon} size={14} />
          <span>{t("manage")}</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-72 p-0">
        <div className="max-h-80 overflow-y-auto p-1">
          {columns.map((column) => {
            const pinned = column.getIsPinned();
            const isVisible = column.getIsVisible();
            // Keep at least one column visible, otherwise the table is unusable.
            const isLastVisible = isVisible && visibleCount === 1;

            return (
              <div
                key={column.id}
                className="flex items-center gap-2 rounded-sm px-2 py-1.5 hover:bg-accent"
              >
                <Checkbox
                  id={`col-${column.id}`}
                  checked={isVisible}
                  disabled={isLastVisible}
                  onCheckedChange={(checked) =>
                    column.toggleVisibility(checked === true)
                  }
                />
                <label
                  htmlFor={`col-${column.id}`}
                  className={cn(
                    "flex-1 cursor-pointer truncate text-sm",
                    isLastVisible && "text-muted-foreground"
                  )}
                >
                  {columnLabel(table, column.id)}
                </label>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6 shrink-0"
                  aria-label={
                    pinned
                      ? t("unpin")
                      : t("pin")
                  }
                  aria-pressed={!!pinned}
                  onClick={() => column.pin(pinned === "left" ? false : "left")}
                >
                  <HugeiconsIcon
                    icon={PinIcon}
                    size={13}
                    className={cn(
                      pinned ? "text-primary" : "text-muted-foreground/60"
                    )}
                  />
                </Button>
              </div>
            );
          })}
        </div>
        {onReset && (
          <>
            <Separator />
            <div className="p-1">
              <Button
                variant="ghost"
                size="sm"
                className="w-full justify-start text-xs"
                onClick={onReset}
              >
                {t("reset")}
              </Button>
            </div>
          </>
        )}
      </PopoverContent>
    </Popover>
  );
}
