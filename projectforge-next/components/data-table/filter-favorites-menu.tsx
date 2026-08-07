"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  AsteriskIcon,
  Bookmark02Icon,
  PlusSignIcon,
} from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import { FilterFavoriteEntry } from "./filter-favorite-entry";
import type { UseFilterFavoritesResult } from "./use-filter-favorites";

interface FilterFavoritesMenuProps {
  favorites: UseFilterFavoritesResult;
  /** Styles the trigger, e.g. to sit at pill height in the filter row. */
  className?: string;
}

/**
 * The user's saved filters for this list: apply one, or save the current filter
 * under a new name.
 *
 * These are the backend's filter favorites, so a filter saved here also shows up
 * in the legacy frontend's list page.
 */
export function FilterFavoritesMenu({
  favorites,
  className,
}: FilterFavoritesMenuProps) {
  const t = useTranslations();
  const tFilter = useTranslations("favorite.filter");
  const [open, setOpen] = useState(false);
  const current = favorites.favorites.find((f) => f.id === favorites.currentId);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          size="sm"
          // The trigger names the applied favorite: the pill row shows its values,
          // but nothing there says which saved filter they came from.
          title={
            current ? `${tFilter("list")}: ${current.name}` : tFilter("list")
          }
          className={cn(className, current && "border-primary/40 text-primary")}
        >
          <HugeiconsIcon icon={Bookmark02Icon} size={13} />
          <span className="max-w-32 truncate">
            {current?.name ?? tFilter("list")}
          </span>
          {/* Says there is something to save without opening the menu. */}
          {current && favorites.isModified && (
            <HugeiconsIcon
              icon={AsteriskIcon}
              size={10}
              aria-label={t("favorites.saveModification")}
            />
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-72 p-1">
        {favorites.favorites.length === 0 ? (
          <p className="px-2 py-1.5 text-xs text-muted-foreground">
            {tFilter("none")}
          </p>
        ) : (
          <div className="max-h-72 overflow-y-auto">
            {favorites.favorites.map((favorite) => (
              <FilterFavoriteEntry
                key={favorite.id}
                favorite={favorite}
                isCurrent={favorite.id === favorites.currentId}
                isModified={favorites.isModified}
                onSelect={() => {
                  setOpen(false);
                  favorites.select(favorite.id);
                }}
                onRename={(newName) => favorites.rename(favorite.id, newName)}
                onUpdate={() => {
                  setOpen(false);
                  favorites.update(favorite.id);
                }}
                onDelete={() => favorites.remove(favorite.id)}
              />
            ))}
          </div>
        )}
        <Separator className="my-1" />
        <SaveAsRow
          placeholder={t("favorite.untitled")}
          label={tFilter("addNew")}
          onSave={(name) => {
            setOpen(false);
            favorites.create(name);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}

/** Saves the filter the list currently uses under a new name. */
function SaveAsRow({
  placeholder,
  label,
  onSave,
}: {
  placeholder: string;
  label: string;
  onSave: (name: string) => void;
}) {
  const [name, setName] = useState("");

  function save() {
    // An empty name is allowed: the backend names it "untitled" (Favorites.getAutoName).
    onSave(name.trim());
    setName("");
  }

  return (
    <div className="flex items-center gap-1 p-1">
      <Input
        value={name}
        placeholder={placeholder}
        aria-label={label}
        onChange={(e) => setName(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") {
            e.preventDefault();
            save();
          }
        }}
        className="h-7 text-xs"
      />
      <Button
        size="sm"
        variant="ghost"
        title={label}
        aria-label={label}
        onClick={save}
        className="h-7 shrink-0 px-2"
      >
        <HugeiconsIcon icon={PlusSignIcon} size={13} />
      </Button>
    </div>
  );
}
