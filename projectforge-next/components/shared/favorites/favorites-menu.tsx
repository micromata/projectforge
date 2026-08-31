"use client";

import { useState, type ReactNode } from "react";
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
import type { FavoriteIdTitle } from "@/lib/rs/types";
import { FavoriteEntry } from "./favorite-entry";

export interface FavoritesMenuProps {
  favorites: FavoriteIdTitle[];
  /** The favorite the values on screen came from, if the caller tracks it. */
  currentId?: number | null;
  /** Whether the current favorite has unsaved changes — a dot on the trigger, and per-entry (see FavoriteEntry). */
  isModified?: boolean;
  onSelect: (id: number) => void;
  onCreate: (name: string) => void;
  onRename: (id: number, newName: string) => void;
  onUpdate?: (id: number) => void;
  onDelete: (id: number) => void;
  /** Extra content above the list — the recent-entries block of the timesheet templates bar. */
  header?: ReactNode;
  /** The trigger's accessible name and, unless icon-only, its label. */
  label?: string;
  /** Renders the trigger label beside the icon rather than as an icon-only button. */
  showLabel?: boolean;
  /**
   * Open as a modal layer. Required when the menu lives inside a modal dialog (the timesheet edit
   * form): a non-modal popover stays outside the dialog's scroll lock (react-remove-scroll), so the
   * wheel never reaches the favorites list and only the scrollbar can be dragged. Defaults to false
   * for standalone use (calendar filters), where a modal scroll lock would be unwanted.
   */
  modal?: boolean;
  className?: string;
}

/**
 * A menu of the user's saved favorites: apply one, save the values on screen under a new name, or
 * rename/overwrite/delete an existing one.
 *
 * Presentational — every write is a callback, so the same menu serves the calendar's filters
 * (`calendar/filter/*`) and a time sheet's templates (`timesheet/favorites/*`), which are different
 * endpoints over the same shape. A Popover, not a dropdown, so the create and rename inputs keep focus
 * instead of closing the menu on the first keystroke.
 */
export function FavoritesMenu({
  favorites,
  currentId,
  isModified,
  onSelect,
  onCreate,
  onRename,
  onUpdate,
  onDelete,
  header,
  label,
  showLabel,
  modal = false,
  className,
}: FavoritesMenuProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const [newName, setNewName] = useState("");
  const triggerLabel = label ?? t("favorites._");

  const commitCreate = () => {
    onCreate(newName.trim());
    setNewName("");
    setOpen(false);
  };

  return (
    <Popover open={open} onOpenChange={setOpen} modal={modal}>
      <PopoverTrigger asChild>
        <Button
          type="button"
          variant="outline"
          size="sm"
          aria-label={triggerLabel}
          className={cn("relative gap-1.5", className)}
        >
          <HugeiconsIcon icon={Bookmark02Icon} size={14} aria-hidden />
          {showLabel && <span className="truncate">{triggerLabel}</span>}
          {isModified && (
            <span
              className="absolute -top-1 -right-1 size-2 rounded-full bg-primary"
              aria-hidden
            />
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-72 p-2">
        {header}
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
            <FavoriteEntry
              key={favorite.id}
              favorite={favorite}
              isCurrent={favorite.id === currentId}
              isModified={isModified}
              onSelect={() => onSelect(favorite.id)}
              onRename={(name) => onRename(favorite.id, name)}
              onUpdate={onUpdate ? () => onUpdate(favorite.id) : undefined}
              onDelete={() => onDelete(favorite.id)}
            />
          ))}
        </div>
      </PopoverContent>
    </Popover>
  );
}
