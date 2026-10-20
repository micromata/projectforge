"use client";

import { useState } from "react";
import { useDroppable } from "@dnd-kit/core";
import { SortableContext, rectSortingStrategy } from "@dnd-kit/sortable";
import { HugeiconsIcon } from "@hugeicons/react";
import { FolderAddIcon } from "@hugeicons/core-free-icons";
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

/**
 * The user's own menu, drawn like the favorites bar it becomes: its entries in a row, each group as its
 * menu button with the menu opened below it. Sortable, and the drop area for the template's entries.
 */
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
    <section className="flex flex-col gap-1.5">
      <h2 className="text-sm font-semibold">
        {t("menu.customizer.customMenuSection")}
      </h2>
      <SortableContext
        items={menu.map((item) => `${FAVORITES}:${itemId(item)}`)}
        strategy={rectSortingStrategy}
      >
        <div
          ref={setNodeRef}
          data-testid="custom-menu"
          className={cn(
            "flex min-h-9 flex-wrap items-start gap-1 rounded-lg border bg-background p-1 shadow-xs",
            isOver && "border-primary bg-primary/5"
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
          {menu.length === 0 && !addingGroup && (
            <p className="self-center px-2 text-xs/relaxed text-muted-foreground">
              <span className="font-medium text-foreground">
                {t("menu.customizer.emptyMenuTitle")}
              </span>{" "}
              {t("menu.customizer.emptyMenuText")}
            </p>
          )}
          <div className="ml-auto flex h-6 items-center">
            {addingGroup ? (
              <GroupNameInput
                label={t("menu.customizer.groupName")}
                onCommit={(name) => {
                  update((m) =>
                    addGroup(m, name, `custom_group_${Date.now()}`)
                  );
                  setAddingGroup(false);
                }}
                onCancel={() => setAddingGroup(false)}
              />
            ) : (
              <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground"
                onClick={() => setAddingGroup(true)}
              >
                <HugeiconsIcon icon={FolderAddIcon} />
                {t("menu.customizer.addGroup")}
              </Button>
            )}
          </div>
        </div>
      </SortableContext>
    </section>
  );
}
