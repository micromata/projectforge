"use client";

import { useState } from "react";
import { useDroppable } from "@dnd-kit/core";
import {
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  Delete02Icon,
  PencilEdit02Icon,
} from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { MenuItem } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { CustomMenuChip } from "./custom-menu-chip";
import { GroupNameInput } from "./group-name-input";
import { itemId } from "./menu-tree";
import { FAVORITES, type DragData } from "./types";

/**
 * A group of the user's menu, drawn as it shows in the bar: its menu button (the handle moving it among
 * the top-level entries, with rename and remove) and its menu opened below, a drop area of its own.
 */
export function CustomMenuGroup({
  group,
  onRename,
  onRemove,
  onRemoveEntry,
}: {
  group: MenuItem;
  onRename: (title: string) => void;
  onRemove: () => void;
  onRemoveEntry: (id: string) => void;
}) {
  const t = useTranslations();
  const [editing, setEditing] = useState(false);
  const id = itemId(group);
  const entries = group.subMenu ?? [];
  const data: DragData = { kind: "group", item: group, container: FAVORITES };
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: `${FAVORITES}:${id}`, data });
  const { setNodeRef: setBodyRef, isOver: bodyIsOver } = useDroppable({
    id: `group:${id}`,
    data: { kind: "item", container: id } satisfies DragData,
  });

  return (
    <div
      ref={setNodeRef}
      data-testid="custom-menu-group"
      // Translate only: the sorting strategy also scales an entry to the size of the one it replaces.
      style={{ transform: CSS.Translate.toString(transform), transition }}
      className={cn(
        "flex w-max max-w-72 min-w-36 flex-col gap-1",
        isDragging && "opacity-40"
      )}
    >
      <div className="flex h-6 items-center gap-0.5 rounded-md bg-muted pr-0.5 text-xs/relaxed font-medium">
        {editing ? (
          <div className="flex min-w-0 flex-1 px-0.5">
            <GroupNameInput
              initial={group.title}
              label={t("menu.customizer.groupName")}
              commitOnBlur
              onCommit={(title) => {
                onRename(title);
                setEditing(false);
              }}
              onCancel={() => setEditing(false)}
            />
          </div>
        ) : (
          <button
            type="button"
            className="flex h-full min-w-0 flex-1 cursor-grab touch-none items-center gap-1 pl-1.5"
            aria-label={`${t("columns.dragToSort")}: ${group.title}`}
            {...attributes}
            {...listeners}
          >
            <span className="truncate">{group.title}</span>
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
          </button>
        )}
        {!editing && (
          <Button
            variant="ghost"
            size="icon-xs"
            className="text-muted-foreground"
            aria-label={`${t("menu.customizer.editGroupName")}: ${group.title}`}
            onClick={() => setEditing(true)}
          >
            <HugeiconsIcon icon={PencilEdit02Icon} className="size-3.5" />
          </Button>
        )}
        <Button
          variant="ghost"
          size="icon-xs"
          className="text-muted-foreground hover:text-destructive"
          aria-label={`${t("menu.customizer.removeGroup")}: ${group.title}`}
          onClick={onRemove}
        >
          <HugeiconsIcon icon={Delete02Icon} className="size-3.5" />
        </Button>
      </div>
      <SortableContext
        id={id}
        items={entries.map((entry) => `${id}:${itemId(entry)}`)}
        strategy={verticalListSortingStrategy}
      >
        {/* The opened menu, styled like the bar's MenubarContent. */}
        <div
          ref={setBodyRef}
          className={cn(
            "flex min-h-9 flex-col rounded-lg border bg-popover p-1 shadow-md",
            bodyIsOver && "border-primary bg-primary/5"
          )}
        >
          {entries.map((entry) => (
            <CustomMenuChip
              key={itemId(entry)}
              item={entry}
              container={id}
              onRemove={() => onRemoveEntry(itemId(entry))}
            />
          ))}
          {entries.length === 0 && (
            <p className="px-2 py-1 text-xs text-muted-foreground">
              {t("menu.customizer.dropItemsHere")}
            </p>
          )}
        </div>
      </SortableContext>
    </div>
  );
}
