"use client";

import { useState } from "react";
import { useDroppable } from "@dnd-kit/core";
import {
  SortableContext,
  rectSortingStrategy,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Delete02Icon,
  DragDropVerticalIcon,
  Folder01Icon,
  PencilEdit02Icon,
} from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { MenuItem } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { CustomMenuChip } from "./custom-menu-chip";
import { GroupNameInput } from "./group-name-input";
import { itemId } from "./menu-tree";
import { FAVORITES, type DragData } from "./types";

/**
 * A group of the user's menu: moved by its handle among the top-level entries, renamed and removed in its
 * header, and a drop area of its own for its entries.
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
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        "w-full rounded-lg border bg-muted/40",
        isDragging && "opacity-40"
      )}
    >
      <div className="flex items-center gap-2 border-b px-2 py-1.5">
        <button
          type="button"
          className="cursor-grab touch-none text-muted-foreground"
          aria-label={`${t("columns.dragToSort")}: ${group.title}`}
          {...attributes}
          {...listeners}
        >
          <HugeiconsIcon icon={DragDropVerticalIcon} size={14} />
        </button>
        <HugeiconsIcon icon={Folder01Icon} size={14} className="text-primary" />
        {editing ? (
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
        ) : (
          <span className="truncate text-sm font-medium">{group.title}</span>
        )}
        <Badge variant="secondary">{entries.length}</Badge>
        <div className="ml-auto flex items-center">
          {!editing && (
            <Button
              variant="ghost"
              size="icon-sm"
              aria-label={`${t("menu.customizer.editGroupName")}: ${group.title}`}
              onClick={() => setEditing(true)}
            >
              <HugeiconsIcon icon={PencilEdit02Icon} />
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon-sm"
            className="hover:text-destructive"
            aria-label={`${t("menu.customizer.removeGroup")}: ${group.title}`}
            onClick={onRemove}
          >
            <HugeiconsIcon icon={Delete02Icon} />
          </Button>
        </div>
      </div>
      <SortableContext
        id={id}
        items={entries.map((entry) => `${id}:${itemId(entry)}`)}
        strategy={rectSortingStrategy}
      >
        <div
          ref={setBodyRef}
          className={cn(
            "flex min-h-11 flex-wrap gap-1.5 p-2",
            bodyIsOver && "bg-primary/5"
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
            <p className="self-center text-xs text-muted-foreground">
              {t("menu.customizer.dropItemsHere")}
            </p>
          )}
        </div>
      </SortableContext>
    </div>
  );
}
