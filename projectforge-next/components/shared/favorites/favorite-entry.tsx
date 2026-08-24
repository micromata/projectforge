"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  AsteriskIcon,
  Cancel01Icon,
  Delete02Icon,
  FloppyDiskIcon,
  PencilEdit02Icon,
  Tick01Icon,
  Tick02Icon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { cn } from "@/lib/utils";
import type { FavoriteIdTitle } from "@/lib/rs/types";

export interface FavoriteEntryProps {
  favorite: FavoriteIdTitle;
  /** True for the favorite the values on screen came from. */
  isCurrent: boolean;
  /**
   * Whether those values differ from the stored ones, where the caller can tell.
   *
   * Given, overwriting is offered on the current entry only — anywhere else it would silently save
   * values the user is not looking at — and its icon says whether there is anything to save: an
   * asterisk while they differ, a check once they match (the vocabulary of the legacy panel,
   * `FavoriteEntry.jsx`). Omitted, every entry offers a plain save, which is what a caller whose
   * values are not comparable can honestly say.
   */
  isModified?: boolean;
  onSelect: () => void;
  onRename: (newName: string) => void;
  /** Omitted where overwriting a favorite is not offered at all. */
  onUpdate?: () => void;
  onDelete: () => void;
}

/**
 * One saved favorite: its name selects it, and the icons beside it overwrite it with the values on
 * screen, rename it in place, or delete it.
 *
 * Renaming swaps the row for an input rather than opening a dialog, so the menu around it stays open —
 * the way the legacy favourites list edited a name.
 */
export function FavoriteEntry({
  favorite,
  isCurrent,
  isModified,
  onSelect,
  onRename,
  onUpdate,
  onDelete,
}: FavoriteEntryProps) {
  const t = useTranslations();
  const [draft, setDraft] = useState<string | null>(null);

  if (draft !== null) {
    const commit = () => {
      const name = draft.trim();
      if (name && name !== favorite.name) onRename(name);
      setDraft(null);
    };
    return (
      <div className="flex items-center gap-1 px-1 py-0.5">
        <Input
          autoFocus
          value={draft}
          aria-label={t("name")}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") commit();
            if (e.key === "Escape") setDraft(null);
          }}
          className="h-7 text-sm"
        />
        <EntryAction icon={Tick02Icon} label={t("save")} onClick={commit} />
        <EntryAction
          icon={Cancel01Icon}
          label={t("cancel")}
          onClick={() => setDraft(null)}
        />
      </div>
    );
  }

  return (
    <div
      className={cn(
        "flex items-center gap-1 rounded-sm px-1 py-0.5",
        isCurrent && "bg-primary/10"
      )}
    >
      <button
        type="button"
        onClick={onSelect}
        aria-current={isCurrent}
        className={cn(
          "min-w-0 flex-1 cursor-pointer truncate rounded px-2 py-1 text-left text-sm hover:bg-accent",
          isCurrent && "font-semibold text-primary"
        )}
      >
        {favorite.name}
      </button>
      {onUpdate && (isModified === undefined || isCurrent) && (
        <EntryAction
          icon={
            isModified === undefined
              ? FloppyDiskIcon
              : isModified
                ? AsteriskIcon
                : Tick01Icon
          }
          label={t(
            isModified === false ? "uptodate" : "favorites.saveModification"
          )}
          // Nothing to save: the check reports a state rather than offering one.
          onClick={isModified === false ? undefined : onUpdate}
          className={isModified ? "text-primary" : undefined}
        />
      )}
      <EntryAction
        icon={PencilEdit02Icon}
        label={`${t("rename")}: ${favorite.name}`}
        onClick={() => setDraft(favorite.name)}
      />
      <EntryAction
        icon={Delete02Icon}
        label={`${t("delete")}: ${favorite.name}`}
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
  onClick?: () => void;
  className?: string;
}) {
  return (
    <HintTooltip text={label}>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        aria-label={label}
        disabled={!onClick}
        onClick={onClick}
        className={cn(
          "size-7 shrink-0 text-muted-foreground hover:text-foreground",
          className
        )}
      >
        <HugeiconsIcon icon={icon} size={14} />
      </Button>
    </HintTooltip>
  );
}
