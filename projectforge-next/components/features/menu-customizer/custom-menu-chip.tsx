"use client";

import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { HugeiconsIcon } from "@hugeicons/react";
import { Cancel01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import type { MenuItem } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { itemId } from "./menu-tree";
import { FAVORITES, type DragData } from "./types";

/** An entry of the user's menu: dragged as a whole, with a button removing it. */
export function CustomMenuChip({
  item,
  container,
  onRemove,
}: {
  item: MenuItem;
  /** [FAVORITES] or the id of the group the entry is in. */
  container: string;
  onRemove: () => void;
}) {
  const t = useTranslations();
  const data: DragData = { kind: "item", item, container };
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: `${container}:${itemId(item)}`, data });
  const removeLabel =
    container === FAVORITES
      ? t("menu.customizer.removeFromFavorites")
      : t("menu.customizer.removeFromGroup");

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        "flex cursor-grab touch-none items-center gap-1.5 rounded-md border bg-card py-1 pl-2.5 pr-1 text-sm shadow-xs",
        isDragging && "opacity-40"
      )}
      {...attributes}
      {...listeners}
    >
      <span className="size-1.5 shrink-0 rounded-full bg-primary" />
      <span className="truncate">{item.title}</span>
      <button
        type="button"
        className="ml-auto rounded-sm p-0.5 text-muted-foreground hover:bg-muted hover:text-destructive"
        aria-label={`${removeLabel}: ${item.title}`}
        // Not the start of a drag, and not the keyboard sensor's Enter/Space.
        onPointerDown={(e) => e.stopPropagation()}
        onKeyDown={(e) => e.stopPropagation()}
        onClick={onRemove}
      >
        <HugeiconsIcon icon={Cancel01Icon} size={12} />
      </button>
    </div>
  );
}
