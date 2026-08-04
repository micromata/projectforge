"use client";

import { useState } from "react";
import type { Column, Table } from "@tanstack/react-table";
import { HugeiconsIcon } from "@hugeicons/react";
import { PinIcon, TableIcon, UnfoldMoreIcon } from "@hugeicons/core-free-icons";
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

/**
 * Plain-text column name. `columnDef.header` renders a component (sort button,
 * filter popover), so `meta.label` carries the text for contexts like this.
 */
function columnLabel<TData>(column: Column<TData, unknown>): string {
  const label = column.columnDef.meta?.label;
  if (label) return label;
  const header = column.columnDef.header;
  return typeof header === "string" ? header : column.id;
}

/** Lets the user show/hide, reorder and pin columns. */
export function DataTableColumnPanel<TData>({
  table,
  onReset,
}: DataTableColumnPanelProps<TData>) {
  const t = useTranslations("columns");
  const [dragId, setDragId] = useState<string | null>(null);

  // Leaf order follows columnOrder, so the list mirrors the table.
  const columns = table.getAllLeafColumns().filter((c) => c.getCanHide());
  const visibleCount = columns.filter((c) => c.getIsVisible()).length;

  /** Moves the dragged column in front of the drop target. */
  function reorder(targetId: string) {
    if (!dragId || dragId === targetId) return;
    const order = table.getAllLeafColumns().map((c) => c.id);
    const from = order.indexOf(dragId);
    const to = order.indexOf(targetId);
    if (from < 0 || to < 0) return;
    order.splice(to, 0, ...order.splice(from, 1));
    table.setColumnOrder(order);
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="gap-1.5">
          <HugeiconsIcon icon={TableIcon} size={14} />
          <span>{t("manage")}</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-72 p-0">
        <p className="px-3 pt-2 text-[11px] text-muted-foreground">
          {t("dragToSort")}
        </p>
        <div className="max-h-80 overflow-y-auto p-1">
          {columns.map((column) => {
            const pinned = column.getIsPinned();
            const isVisible = column.getIsVisible();
            // Keep at least one column visible, otherwise the table is unusable.
            const isLastVisible = isVisible && visibleCount === 1;

            return (
              <div
                key={column.id}
                draggable
                onDragStart={() => setDragId(column.id)}
                onDragEnd={() => setDragId(null)}
                onDragOver={(e) => e.preventDefault()}
                onDrop={(e) => {
                  e.preventDefault();
                  reorder(column.id);
                  setDragId(null);
                }}
                className={cn(
                  "flex items-center gap-1.5 rounded-sm px-2 py-1.5 hover:bg-accent",
                  dragId === column.id && "opacity-40"
                )}
              >
                <HugeiconsIcon
                  icon={UnfoldMoreIcon}
                  size={13}
                  className="shrink-0 cursor-grab text-muted-foreground/60"
                />
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
                  {columnLabel(column)}
                </label>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-6 w-6 shrink-0"
                  aria-label={pinned ? t("unpin") : t("pin")}
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
