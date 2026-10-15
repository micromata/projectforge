"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Bookmark02Icon, PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import type { CalendarFilterFavorite } from "@/lib/rs/calendar-types";
import { useCalendarFavorites } from "./use-calendar-favorites";
import { CalendarFavoriteEntry } from "./calendar-favorite-entry";

interface CalendarFavoritesMenuProps {
  favorites: CalendarFilterFavorite[];
  currentFilterId?: number | null;
  /** True once the current filter differs from its saved form — shown as a dot on the trigger. */
  isFilterModified: boolean;
}

/**
 * The saved calendar filters. A dot on the trigger marks that the current filter has unsaved changes
 * (`isFilterModified`); the top row saves the current filter under a new name, each entry below selects,
 * overwrites, renames or deletes one (see calendar-favorite-entry). A Popover, not a dropdown menu, so
 * the create and rename inputs keep focus instead of closing the menu on the first keystroke.
 */
export function CalendarFavoritesMenu({
  favorites,
  currentFilterId,
  isFilterModified,
}: CalendarFavoritesMenuProps) {
  const t = useTranslations();
  const { create, update, rename, remove, select } = useCalendarFavorites();
  const [newName, setNewName] = useState("");

  const commitCreate = () => {
    const name = newName.trim();
    if (!name) return;
    create(name);
    setNewName("");
  };

  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="relative h-7 gap-1.5"
        >
          <HugeiconsIcon icon={Bookmark02Icon} size={14} aria-hidden />
          {/* `favorites` is both a label and the parent of `favorites.saveModification`, so the bundle
              exports it as a subtree with the label on a `_` leaf. */}
          <span>{t("favorites._")}</span>
          {isFilterModified && (
            <span
              className="absolute -top-1 -right-1 size-2 rounded-full bg-primary"
              aria-hidden
            />
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-72 p-2">
        <div className="flex items-center gap-1">
          <Input
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && commitCreate()}
            placeholder={t("favorite.addNew")}
            className="h-7 text-sm"
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={t("favorite.addNew")}
            disabled={!newName.trim()}
            onClick={commitCreate}
            className="size-7 shrink-0"
          >
            <HugeiconsIcon icon={PlusSignIcon} size={14} />
          </Button>
        </div>
        {favorites.length > 0 && <Separator className="my-2" />}
        <div
          className={cn(
            "flex flex-col",
            favorites.length > 6 && "max-h-72 overflow-auto"
          )}
        >
          {favorites.map((favorite) => (
            <CalendarFavoriteEntry
              key={favorite.id}
              favorite={favorite}
              isCurrent={favorite.id === currentFilterId}
              onSelect={select}
              onRename={rename}
              onUpdate={update}
              onDelete={remove}
            />
          ))}
        </div>
      </PopoverContent>
    </Popover>
  );
}
