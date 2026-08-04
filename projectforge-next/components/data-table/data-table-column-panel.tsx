"use client";

import { useState } from "react";
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

  const hideable = table.getAllLeafColumns().filter((c) => c.getCanHide());
  const visibleCount = hideable.filter((c) => c.getIsVisible()).length;

  // Pinned columns lead the table, so the list shows them first and outside the
  // sortable area; the order within each group follows columnOrder.
  const pinned = hideable.filter((c) => c.getIsPinned());
  const sortable = hideable.filter((c) => !c.getIsPinned());
  const sortableIds = sortable.map((c) => c.id);

  const sensors = useSensors(
    // A small threshold so clicking the checkbox isn't swallowed by a drag.
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  function handleDragEnd({ active, over }: DragEndEvent) {
    setDragId(null);
    if (!over || active.id === over.id) return;

    const from = sortableIds.indexOf(String(active.id));
    const to = sortableIds.indexOf(String(over.id));
    if (from < 0 || to < 0) return;

    // Reorder within the unpinned group, then rebuild the full column order so
    // pinned columns keep their leading positions.
    const reordered = arrayMove(sortableIds, from, to);
    table.setColumnOrder([...pinned.map((c) => c.id), ...reordered]);
  }

  const draggedColumn = dragId
    ? sortable.find((c) => c.id === dragId)
    : undefined;

  /**
   * Pins or unpins and moves the column accordingly. `column.pin()` only touches
   * the pinning state, but the table renders in column order — so a pinned column
   * would otherwise stick at its old position instead of leading the table.
   */
  function togglePin(column: Column<TData, unknown>) {
    const isPinned = !!column.getIsPinned();
    const others = table
      .getAllLeafColumns()
      .map((c) => c.id)
      .filter((id) => id !== column.id);

    if (isPinned) {
      column.pin(false);
      // Back behind the remaining pinned columns.
      const stillPinned = pinned.filter((c) => c.id !== column.id).length;
      others.splice(stillPinned, 0, column.id);
    } else {
      column.pin("left");
      others.unshift(column.id);
    }
    table.setColumnOrder(others);
  }

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="gap-1.5">
          <HugeiconsIcon icon={TableIcon} size={14} />
          <span>{t("manage")}</span>
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80 p-0">
        <div className="max-h-96 overflow-y-auto p-1">
          {pinned.length > 0 && (
            <>
              {pinned.map((column) => (
                <ColumnRow
                  key={column.id}
                  column={column}
                  label={columnLabel(column)}
                  isLastVisible={column.getIsVisible() && visibleCount === 1}
                  pinLabel={t("unpin")}
                  onTogglePin={() => togglePin(column)}
                />
              ))}
              <Separator className="my-1" />
            </>
          )}
          <p className="px-2 pb-1 text-[11px] text-muted-foreground">
            {t("dragToSort")}
          </p>
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
            <SortableContext
              items={sortableIds}
              strategy={verticalListSortingStrategy}
            >
              {sortable.map((column) => (
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
            {/* Follows the cursor so it's obvious what is being moved. */}
            <DragOverlay>
              {draggedColumn && (
                <div className="flex items-center gap-1.5 rounded-sm border bg-popover px-2 py-1.5 shadow-lg">
                  <HugeiconsIcon
                    icon={UnfoldMoreIcon}
                    size={13}
                    className="text-muted-foreground"
                  />
                  <span className="text-sm">{columnLabel(draggedColumn)}</span>
                </div>
              )}
            </DragOverlay>
          </DndContext>
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

interface ColumnRowProps<TData> {
  column: Column<TData, unknown>;
  label: string;
  isLastVisible: boolean;
  pinLabel: string;
  dragLabel?: string;
  onTogglePin: () => void;
}

/** A pinned column: visible and unpinnable, but not reorderable. */
function ColumnRow<TData>({
  column,
  label,
  isLastVisible,
  pinLabel,
  onTogglePin,
}: ColumnRowProps<TData>) {
  return (
    <div className="flex items-center gap-1.5 rounded-sm px-2 py-1.5 hover:bg-accent">
      <span className="w-[13px] shrink-0" aria-hidden />
      <ColumnRowControls
        column={column}
        label={label}
        isLastVisible={isLastVisible}
        pinLabel={pinLabel}
        onTogglePin={onTogglePin}
      />
    </div>
  );
}

function SortableColumnRow<TData>({
  column,
  label,
  isLastVisible,
  pinLabel,
  dragLabel,
  onTogglePin,
}: ColumnRowProps<TData>) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: column.id });

  return (
    <div
      ref={setNodeRef}
      style={{
        transform: CSS.Transform.toString(transform),
        transition: isDragging ? "none" : (transition ?? undefined),
      }}
      className={cn(
        "flex items-center gap-1.5 rounded-sm px-2 py-1.5 hover:bg-accent",
        // Leave a gap where the row will land; the overlay shows the row itself.
        isDragging && "opacity-0"
      )}
    >
      <button
        type="button"
        className="shrink-0 cursor-grab touch-none text-muted-foreground/60 active:cursor-grabbing"
        aria-label={dragLabel ? `${label}: ${dragLabel}` : label}
        {...attributes}
        {...listeners}
      >
        <HugeiconsIcon icon={UnfoldMoreIcon} size={13} />
      </button>
      <ColumnRowControls
        column={column}
        label={label}
        isLastVisible={isLastVisible}
        pinLabel={pinLabel}
        onTogglePin={onTogglePin}
      />
    </div>
  );
}

/** Checkbox, label and pin toggle — identical for pinned and sortable rows. */
function ColumnRowControls<TData>({
  column,
  label,
  isLastVisible,
  pinLabel,
  onTogglePin,
}: ColumnRowProps<TData>) {
  const isPinned = !!column.getIsPinned();

  return (
    <>
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
    </>
  );
}
