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

/**
 * An entry of the user's menu, dragged as a whole: a menu button in the bar, a menu item inside a group.
 * The button removing it shows on hover and focus, and always on a touch screen.
 */
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
  const topLevel = container === FAVORITES;
  const removeLabel = topLevel
    ? t("menu.customizer.removeFromFavorites")
    : t("menu.customizer.removeFromGroup");

  return (
    <div
      ref={setNodeRef}
      // Translate only: the sorting strategy also scales an entry to the size of the one it replaces.
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={cn(
        "group/entry flex cursor-grab touch-none items-center gap-1 rounded-md text-xs/relaxed hover:bg-muted",
        // The sizes of the bar's ENTRY_CLASS (FavoritesBar) and of a MenubarItem.
        topLevel ? "h-6 pr-0.5 pl-1.5 font-medium" : "min-h-7 py-1 pr-1 pl-2",
        isDragging && "opacity-40"
      )}
      {...attributes}
      {...listeners}
    >
      <span className="truncate">{item.title}</span>
      <button
        type="button"
        className="ml-auto rounded-sm p-0.5 text-muted-foreground hover:bg-background hover:text-destructive focus-visible:opacity-100 group-hover/entry:opacity-100 pointer-fine:opacity-0"
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
