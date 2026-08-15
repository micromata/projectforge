"use client";

import { useState } from "react";
import { createPortal } from "react-dom";
import type { Column, Table } from "@tanstack/react-table";
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { restrictToVerticalAxis } from "@dnd-kit/modifiers";
import {
  SortableContext,
  arrayMove,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
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
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";

interface DataTableColumnPanelProps<TData> {
  table: Table<TData>;
  onReset?: () => void;
  /** Styles the trigger, e.g. to sit at pill height in a filter row. */
  className?: string;
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
  className,
}: DataTableColumnPanelProps<TData>) {
  const t = useTranslations("columns");
  const [dragId, setDragId] = useState<string | null>(null);

  const allColumns = table.getAllLeafColumns();
  // getAllLeafColumns returns the columns as defined, while the table renders by
  // columnOrder. Both listing and reordering have to follow the order the user
  // actually sees, and every setColumnOrder below has to carry all columns —
  // rebuilding the order from a subset silently drops the rest out of it.
  const order = table.getState().columnOrder;
  const rank = (id: string) => {
    const i = order.indexOf(id);
    // Unordered columns keep their defined order behind the ordered ones (sort is stable).
    return i < 0 ? allColumns.length : i;
  };
  const ordered = order.length
    ? [...allColumns].sort((a, b) => rank(a.id) - rank(b.id))
    : allColumns;
  const orderedIds = ordered.map((c) => c.id);

  // Counts *every* left-pinned column, not just the listed ones: a column with
  // `enableHiding: false` (the task tree's structure column) is pinned and holds
  // a leading slot in the order all the same. Counting only the listed ones puts
  // a newly pinned column ahead of it, and since a pinned cell takes its sticky
  // offset from the pinning order while it renders by columnOrder, the two
  // disagree and the pinned columns overlap.
  const leftPinnedIds = ordered
    .filter((c) => c.getIsPinned() === "left")
    .map((c) => c.id);

  const hideable = ordered.filter((c) => c.getCanHide());
  const visibleCount = hideable.filter((c) => c.getIsVisible()).length;

  // Pinned columns lead the table, so they form their own group here. Dragging
  // sorts within a group; moving between groups is what the pin toggle does.
  const pinned = hideable.filter((c) => c.getIsPinned());
  const unpinned = hideable.filter((c) => !c.getIsPinned());
  const pinnedIds = pinned.map((c) => c.id);
  const unpinnedIds = unpinned.map((c) => c.id);

  const sensors = useSensors(
    // A small threshold so clicking the checkbox isn't swallowed by a drag.
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  function handleDragEnd({ active, over }: DragEndEvent) {
    setDragId(null);
    if (!over || active.id === over.id) return;

    const activeId = String(active.id);
    const overId = String(over.id);
    const group = pinnedIds.includes(activeId) ? pinnedIds : unpinnedIds;

    // Ignore drops onto the other group — pinning moves columns between them.
    if (!group.includes(activeId) || !group.includes(overId)) return;

    // Moved within the full order rather than within the group, so columns the
    // panel doesn't list (not hideable) keep their place instead of being
    // dropped from the order entirely.
    const from = orderedIds.indexOf(activeId);
    const to = orderedIds.indexOf(overId);
    if (from < 0 || to < 0) return;
    table.setColumnOrder(arrayMove(orderedIds, from, to));
  }

  /**
   * Pins or unpins and moves the column accordingly. `column.pin()` only touches
   * the pinning state, but the table renders in column order — so a pinned column
   * would otherwise stick at its old position instead of leading the table.
   */
  function togglePin(column: Column<TData, unknown>) {
    const isPinned = !!column.getIsPinned();
    const others = orderedIds.filter((id) => id !== column.id);
    // How many left-pinned columns remain once this one has moved — the slot the
    // column has to take so pinning order and column order stay in step.
    const remaining = leftPinnedIds.filter((id) => id !== column.id).length;

    column.pin(isPinned ? false : "left");
    // The same slot either way: as the last pinned column, or as the first
    // scrolling one right behind the columns that stay pinned.
    others.splice(remaining, 0, column.id);
    table.setColumnOrder(others);
  }

  const draggedColumn = dragId
    ? hideable.find((c) => c.id === dragId)
    : undefined;

  return (
    <Popover>
      {/* Short label, full one as the tooltip: this sits in the filter row, not in a menu. The
          tooltip wraps the popover trigger, not the other way round — `asChild` has to reach a DOM
          element, and a Radix root is not one. */}
      <HintTooltip text={t("manage")}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            size="sm"
            className={cn("gap-1.5", className)}
          >
            <HugeiconsIcon icon={TableIcon} size={14} />
            <span>{t("_")}</span>
          </Button>
        </PopoverTrigger>
      </HintTooltip>
      <PopoverContent align="end" className="w-80 p-0">
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          modifiers={[restrictToVerticalAxis]}
          onDragStart={({ active }: DragStartEvent) =>
            setDragId(String(active.id))
          }
          onDragEnd={handleDragEnd}
          onDragCancel={() => setDragId(null)}
        >
          <div className="max-h-96 overflow-y-auto p-1">
            {pinned.length > 0 && (
              <div className="mb-1 rounded-sm bg-primary/5 p-1">
                <p className="flex items-center gap-1 px-1 pb-1 text-[11px] font-medium text-primary">
                  <HugeiconsIcon icon={PinIcon} size={11} />
                  {t("pinned")}
                </p>
                <SortableContext
                  items={pinnedIds}
                  strategy={verticalListSortingStrategy}
                >
                  {pinned.map((column) => (
                    <SortableColumnRow
                      key={column.id}
                      column={column}
                      label={columnLabel(column)}
                      isLastVisible={
                        column.getIsVisible() && visibleCount === 1
                      }
                      pinLabel={t("unpin")}
                      dragLabel={t("dragToSort")}
                      onTogglePin={() => togglePin(column)}
                    />
                  ))}
                </SortableContext>
              </div>
            )}
            <p className="px-2 pb-1 text-[11px] text-muted-foreground">
              {t("dragToSort")}
            </p>
            <SortableContext
              items={unpinnedIds}
              strategy={verticalListSortingStrategy}
            >
              {unpinned.map((column) => (
                <SortableColumnRow
                  key={column.id}
                  column={column}
                  label={columnLabel(column)}
                  isLastVisible={column.getIsVisible() && visibleCount === 1}
                  pinLabel={t("pin")}
                  dragLabel={t("dragToSort")}
                  onTogglePin={() => togglePin(column)}
                />
              ))}
            </SortableContext>
          </div>
          {/* Portalled to the body: the popover is positioned with a CSS
              transform, which offsets the overlay's fixed positioning and would
              leave the dragged row somewhere off-screen. */}
          {typeof document !== "undefined" &&
            createPortal(
              <DragOverlay>
                {draggedColumn && (
                  <div className="flex items-center gap-1.5 rounded-sm border bg-popover px-2 py-1.5 text-sm shadow-lg">
                    <HugeiconsIcon
                      icon={UnfoldMoreIcon}
                      size={13}
                      className="text-muted-foreground"
                    />
                    <span>{columnLabel(draggedColumn)}</span>
                  </div>
                )}
              </DragOverlay>,
              document.body
            )}
        </DndContext>
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

interface ColumnRowProps<TData> {
  column: Column<TData, unknown>;
  label: string;
  isLastVisible: boolean;
  pinLabel: string;
  dragLabel: string;
  onTogglePin: () => void;
}

function SortableColumnRow<TData>({
  column,
  label,
  isLastVisible,
  pinLabel,
  dragLabel,
  onTogglePin,
}: ColumnRowProps<TData>) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: column.id });
  const isPinned = !!column.getIsPinned();

  return (
    <div
      ref={setNodeRef}
      style={{
        transform: CSS.Transform.toString(transform),
        transition: isDragging ? "none" : (transition ?? undefined),
      }}
      className={cn(
        "flex items-center gap-1.5 rounded-sm px-2 py-1.5",
        // Dashed outline marks the slot the row will drop into, while the overlay
        // shows the row itself following the cursor.
        isDragging
          ? "bg-muted/50 outline-1 outline-dashed outline-primary"
          : "hover:bg-accent/60"
      )}
    >
      <button
        type="button"
        className="shrink-0 cursor-grab touch-none text-muted-foreground/60 active:cursor-grabbing"
        aria-label={`${label}: ${dragLabel}`}
        {...attributes}
        {...listeners}
      >
        <HugeiconsIcon icon={UnfoldMoreIcon} size={13} />
      </button>
      <Checkbox
        id={`col-${column.id}`}
        checked={column.getIsVisible()}
        disabled={isLastVisible}
        onCheckedChange={(checked) => column.toggleVisibility(checked === true)}
      />
      <label
        htmlFor={`col-${column.id}`}
        className={cn(
          "flex-1 cursor-pointer truncate text-sm",
          isLastVisible && "text-muted-foreground"
        )}
      >
        {label}
      </label>
      <Button
        variant="ghost"
        size="icon"
        className="h-6 w-6 shrink-0"
        aria-label={pinLabel}
        aria-pressed={isPinned}
        onClick={onTogglePin}
      >
        <HugeiconsIcon
          icon={PinIcon}
          size={13}
          className={cn(isPinned ? "text-primary" : "text-muted-foreground/60")}
        />
      </Button>
    </div>
  );
}
