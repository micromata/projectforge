"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  AsteriskIcon,
  Delete02Icon,
  PencilEdit02Icon,
  Tick01Icon,
} from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import type { FavoriteIdTitle } from "@/lib/rs/types";

interface FilterFavoriteEntryProps {
  favorite: FavoriteIdTitle;
  /** True for the favorite whose values the list currently uses. */
  isCurrent: boolean;
  onSelect: () => void;
  onRename: (newName: string) => void;
  onUpdate: () => void;
  onDelete: () => void;
}

/**
 * One saved filter: click to apply, plus rename, overwrite and delete.
 *
 * "Overwrite with the current filter" is only offered on the applied favorite —
 * anywhere else it would silently save values the user isn't looking at.
 */
export function FilterFavoriteEntry({
  favorite,
  isCurrent,
  onSelect,
  onRename,
  onUpdate,
  onDelete,
}: FilterFavoriteEntryProps) {
  const t = useTranslations();
  const [renaming, setRenaming] = useState(false);

  if (renaming) {
    return (
      <RenameRow
        name={favorite.name}
        onCancel={() => setRenaming(false)}
        onSave={(newName) => {
          setRenaming(false);
          if (newName && newName !== favorite.name) onRename(newName);
        }}
      />
    );
  }

  return (
    <div
      className={cn(
        "group flex items-center gap-1 rounded-sm pr-1 hover:bg-accent",
        isCurrent && "bg-primary/10"
      )}
    >
      <button
        type="button"
        onClick={onSelect}
        aria-current={isCurrent}
        aria-label={t("favorite.filter.select", { arg0: favorite.name })}
        className={cn(
          "min-w-0 flex-1 cursor-pointer truncate px-2 py-1 text-left text-xs",
          isCurrent && "font-medium text-primary"
        )}
      >
        {favorite.name}
      </button>
      {isCurrent && (
        <EntryAction
          icon={AsteriskIcon}
          label={t("favorites.saveModification")}
          onClick={onUpdate}
        />
      )}
      <EntryAction
        icon={PencilEdit02Icon}
        label={t("rename")}
        onClick={() => setRenaming(true)}
      />
      <EntryAction
        icon={Delete02Icon}
        label={t("delete")}
        onClick={onDelete}
        className="hover:text-destructive"
      />
    </div>
  );
}

function EntryAction({
  icon,
  label,
  onClick,
  className,
}: {
  icon: typeof Tick01Icon;
  label: string;
  onClick: () => void;
  className?: string;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={label}
      aria-label={label}
      className={cn(
        "shrink-0 cursor-pointer rounded-sm p-1 text-muted-foreground hover:text-foreground",
        className
      )}
    >
      <HugeiconsIcon icon={icon} size={12} />
    </button>
  );
}

/** Inline name editor, shared shape with the "save as" row of the panel. */
function RenameRow({
  name,
  onSave,
  onCancel,
}: {
  name: string;
  onSave: (name: string) => void;
  onCancel: () => void;
}) {
  const t = useTranslations();
  const [value, setValue] = useState(name);

  return (
    <div className="flex items-center gap-1 py-0.5">
      <Input
        autoFocus
        value={value}
        aria-label={t("name")}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") onSave(value.trim());
          if (e.key === "Escape") onCancel();
        }}
        onBlur={onCancel}
        className="h-7 text-xs"
      />
      {/* mousedown, not click: the input's blur cancels the edit before a click would land. */}
      <button
        type="button"
        title={t("save")}
        aria-label={t("save")}
        onMouseDown={(e) => {
          e.preventDefault();
          onSave(value.trim());
        }}
        className="shrink-0 cursor-pointer rounded-sm p-1 text-muted-foreground hover:text-foreground"
      >
        <HugeiconsIcon icon={Tick01Icon} size={12} />
      </button>
    </div>
  );
}
