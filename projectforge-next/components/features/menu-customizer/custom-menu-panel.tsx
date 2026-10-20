"use client";

import { useState } from "react";
import { useDroppable } from "@dnd-kit/core";
import { SortableContext, rectSortingStrategy } from "@dnd-kit/sortable";
import { HugeiconsIcon } from "@hugeicons/react";
import { Folder01Icon, FolderAddIcon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import type { MenuItem } from "@/lib/rs/types";
import { cn } from "@/lib/utils";
import { CustomMenuChip } from "./custom-menu-chip";
import { CustomMenuGroup } from "./custom-menu-group";
import { GroupNameInput } from "./group-name-input";
import {
  addGroup,
  isGroup,
  itemId,
  removeItem,
  renameGroup,
} from "./menu-tree";
import { FAVORITES, type DragData } from "./types";
import { CustomizerPanel } from "./customizer-panel";

/** The user's own menu: its entries and groups, sortable, and the drop area for the template's entries. */
export function CustomMenuPanel({
  menu,
  update,
}: {
  menu: MenuItem[];
  update: (change: (menu: MenuItem[]) => MenuItem[]) => void;
}) {
  const t = useTranslations();
  const [addingGroup, setAddingGroup] = useState(false);
  const { setNodeRef, isOver } = useDroppable({
    id: FAVORITES,
    data: { kind: "item", container: FAVORITES } satisfies DragData,
  });

  return (
    <CustomizerPanel
      title={t("menu.customizer.customMenuSection")}
      actions={
        addingGroup ? (
          <GroupNameInput
            label={t("menu.customizer.groupName")}
            onCommit={(name) => {
              update((m) => addGroup(m, name, `custom_group_${Date.now()}`));
              setAddingGroup(false);
            }}
            onCancel={() => setAddingGroup(false)}
          />
        ) : (
          <Button
            variant="outline"
            size="sm"
            onClick={() => setAddingGroup(true)}
          >
            <HugeiconsIcon icon={FolderAddIcon} />
            {t("menu.customizer.addGroup")}
          </Button>
        )
      }
    >
      <SortableContext
        items={menu.map((item) => `${FAVORITES}:${itemId(item)}`)}
        strategy={rectSortingStrategy}
      >
        <div
          ref={setNodeRef}
          data-testid="custom-menu"
          className={cn(
            "flex min-h-40 flex-1 flex-wrap content-start gap-2 rounded-md p-1",
            isOver && "bg-primary/5"
          )}
        >
          {menu.map((item) =>
            isGroup(item) ? (
              <CustomMenuGroup
                key={itemId(item)}
                group={item}
                onRename={(title) =>
                  update((m) => renameGroup(m, itemId(item), title))
                }
                onRemove={() => update((m) => removeItem(m, itemId(item)))}
                onRemoveEntry={(id) =>
                  update((m) => removeItem(m, id, itemId(item)))
                }
              />
            ) : (
              <CustomMenuChip
                key={itemId(item)}
                item={item}
                container={FAVORITES}
                onRemove={() => update((m) => removeItem(m, itemId(item)))}
              />
            )
          )}
          {menu.length === 0 && (
            <div className="flex w-full flex-col items-center gap-1 py-10 text-center text-muted-foreground">
              <HugeiconsIcon icon={Folder01Icon} size={28} />
              <p className="text-sm font-medium text-foreground">
                {t("menu.customizer.emptyMenuTitle")}
              </p>
              <p className="max-w-xs text-xs">
                {t("menu.customizer.emptyMenuText")}
              </p>
            </div>
          )}
        </div>
      </SortableContext>
    </CustomizerPanel>
  );
}
