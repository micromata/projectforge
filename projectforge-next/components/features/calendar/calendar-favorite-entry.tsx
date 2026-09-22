"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Cancel01Icon,
  Delete02Icon,
  FloppyDiskIcon,
  PencilEdit02Icon,
  Tick02Icon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import type { CalendarFilterFavorite } from "@/lib/rs/calendar-types";

interface CalendarFavoriteEntryProps {
  favorite: CalendarFilterFavorite;
  isCurrent: boolean;
  onSelect: (id: number) => void;
  onRename: (id: number, newName: string) => void;
  onUpdate: (id: number) => void;
  onDelete: (id: number) => void;
}

/**
 * One saved filter: its name selects it, and three icon actions overwrite it with the current filter
 * (`updateFilter`), rename it in place, or delete it. Renaming swaps the row for an input so the whole
 * menu need not close for it — the way the legacy favourites list edited a name inline.
 */
export function CalendarFavoriteEntry({
  favorite,
  isCurrent,
  onSelect,
  onRename,
  onUpdate,
  onDelete,
}: CalendarFavoriteEntryProps) {
  const t = useTranslations();
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(favorite.name);

  const commitRename = () => {
    const name = draft.trim();
    if (name && name !== favorite.name) onRename(favorite.id, name);
    setEditing(false);
  };

  if (editing) {
    return (
      <div className="flex items-center gap-1 px-1 py-0.5">
        <Input
          value={draft}
          autoFocus
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") commitRename();
            if (e.key === "Escape") setEditing(false);
          }}
          className="h-7 text-sm"
        />
        <IconButton
          icon={Tick02Icon}
          label={t("save")}
          onClick={commitRename}
        />
        <IconButton
          icon={Cancel01Icon}
          label={t("cancel")}
          onClick={() => setEditing(false)}
        />
      </div>
    );
  }

  return (
    <div className="group flex items-center gap-1 px-1 py-0.5">
      <button
        type="button"
        onClick={() => onSelect(favorite.id)}
        className={cn(
          "flex-1 cursor-pointer truncate rounded px-2 py-1 text-left text-sm hover:bg-accent",
          isCurrent && "font-semibold text-primary"
        )}
      >
        {favorite.name}
      </button>
      <IconButton
        icon={FloppyDiskIcon}
        label={`${t("save")}: ${favorite.name}`}
        onClick={() => onUpdate(favorite.id)}
      />
      <IconButton
        icon={PencilEdit02Icon}
        label={`${t("rename")}: ${favorite.name}`}
        onClick={() => {
          setDraft(favorite.name);
          setEditing(true);
        }}
      />
      <IconButton
        icon={Delete02Icon}
        label={`${t("delete")}: ${favorite.name}`}
        onClick={() => onDelete(favorite.id)}
      />
    </div>
  );
}

function IconButton({
  icon,
  label,
  onClick,
}: {
  icon: typeof Tick02Icon;
  label: string;
  onClick: () => void;
}) {
  return (
    <Button
      type="button"
      variant="ghost"
      size="icon"
      aria-label={label}
      onClick={onClick}
      className="size-7 shrink-0 text-muted-foreground hover:text-foreground"
    >
      <HugeiconsIcon icon={icon} size={14} />
    </Button>
  );
}
