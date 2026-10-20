"use client";

import { useDraggable } from "@dnd-kit/core";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { MenuItem } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { itemId } from "./menu-tree";
import { TEMPLATE, type DragData } from "./types";

/**
 * An entry of the template, one line as in the main menu: a button adding it to the end of the user's
 * menu, and draggable to a place of its own choosing. Only the pointer drags it — Enter and Space stay the
 * button's, so the keyboard adds. An entry already in the menu is shown ticked and does neither.
 */
export function TemplateChip({
  item,
  added,
  onAdd,
}: {
  item: MenuItem;
  added: boolean;
  onAdd: () => void;
}) {
  const t = useTranslations();
  const data: DragData = { kind: "item", item, container: TEMPLATE };
  const { listeners, setNodeRef, isDragging } = useDraggable({
    id: `${TEMPLATE}:${itemId(item)}`,
    data,
    disabled: added,
  });

  if (added) {
    return (
      <HintTooltip text={t("menu.customizer.alreadyAdded")} openOnTap>
        <span className="flex items-center gap-1.5 px-2 py-0.5 text-xs/relaxed text-muted-foreground">
          <HugeiconsIcon
            icon={Tick02Icon}
            size={12}
            className="shrink-0 text-primary"
          />
          <span className="truncate">{item.title}</span>
        </span>
      </HintTooltip>
    );
  }
  return (
    <button
      ref={setNodeRef}
      type="button"
      data-testid="template-entry"
      className={cn(
        "flex w-full cursor-grab touch-none items-center rounded-sm py-0.5 pr-2 pl-[1.625rem] text-left text-xs/relaxed hover:bg-muted",
        isDragging && "opacity-40"
      )}
      {...listeners}
      // The keyboard sensor's activator: Enter and Space click instead.
      onKeyDown={undefined}
      onClick={onAdd}
    >
      <span className="truncate">{item.title}</span>
    </button>
  );
}
