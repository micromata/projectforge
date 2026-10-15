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
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";
import type { FavoriteIdTitle } from "@/lib/rs/types";

interface FilterFavoriteEntryProps {
  favorite: FavoriteIdTitle;
  /** True for the favorite the list's values are based on. */
  isCurrent: boolean;
  /** Whether those values differ from the stored ones, i.e. can be saved. */
  isModified: boolean;
  onSelect: () => void;
  onRename: (newName: string) => void;
  onUpdate: () => void;
  onDelete: () => void;
}

/**
 * One saved filter: click to apply, plus rename, overwrite and delete.
 *
 * "Overwrite with the current filter" is only offered on the favorite the values
 * are based on — anywhere else it would silently save values the user isn't
 * looking at. Its icon says whether there is anything to save: an asterisk while
 * the values differ, a check once they match the stored ones (same vocabulary as
 * the legacy panel, `FavoriteEntry.jsx`).
 */
export function FilterFavoriteEntry({
  favorite,
  isCurrent,
  isModified,
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
          icon={isModified ? AsteriskIcon : Tick01Icon}
          label={t(isModified ? "favorites.saveModification" : "uptodate")}
          // Nothing to save: the check is a state, not an offer.
          onClick={isModified ? onUpdate : undefined}
          className={isModified ? "text-primary" : undefined}
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
  /** Omitted for an icon that only reports a state, e.g. "up to date". */
  onClick?: () => void;
  className?: string;
}) {
  return (
    <HintTooltip text={label}>
      <button
        type="button"
        onClick={onClick}
        disabled={!onClick}
        aria-label={label}
        className={cn(
          "shrink-0 rounded-sm p-1 text-muted-foreground",
          onClick ? "cursor-pointer hover:text-foreground" : "cursor-default",
          className
        )}
      >
        <HugeiconsIcon icon={icon} size={12} />
      </button>
    </HintTooltip>
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
      <HintTooltip text={t("save")}>
        <button
          type="button"
          aria-label={t("save")}
          onMouseDown={(e) => {
            e.preventDefault();
            onSave(value.trim());
          }}
          className="shrink-0 cursor-pointer rounded-sm p-1 text-muted-foreground hover:text-foreground"
        >
          <HugeiconsIcon icon={Tick01Icon} size={12} />
        </button>
      </HintTooltip>
    </div>
  );
}
