"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FloppyDiskIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampMinutes } from "@/lib/format";

export interface EntityEditActionsProps {
  onCancel: () => void;
  /**
   * Right of the save button — a choice about the save rather than about the entity (an order's
   * "send an e-mail notification?", see `EditDef.saveOption`).
   */
  saveOption?: ReactNode;
  /**
   * Delete button, rendered right-aligned. Omitted while the entry doesn't exist yet — deleting it
   * needs the saved entity (see [EntityDeleteButton]).
   */
  deleteAction?: ReactNode;
  isSaving: boolean;
  isDirty: boolean;
  /**
   * When the entry was last written, as the ISO timestamp the backend sends (`created`,
   * `lastUpdate`). Formatted here in the user's locale and time zone — the raw value would read
   * "2003-01-25T08:39:29.000Z". Null for a new entry, which has no such moment yet.
   */
  lastSaved: string | null;
}

/**
 * The bottom bar of every edit page: cancel, save, when it was last saved, and the delete button.
 *
 * `save` and `cancel` are the backend's own labels, so they read the same here as in Wicket and the
 * legacy pages; only the "last saved" line has no backend counterpart and comes from the
 * hand-written catalog. It lives under `entityEdit`, not `edit`: the backend exports `edit` as a
 * leaf ("Bearbeiten"), and a hand-written object of that name would replace the string and break
 * every `t("edit")` (see mergeMessages in i18n/config.ts).
 */
export function EntityEditActions({
  onCancel,
  saveOption,
  deleteAction,
  isSaving,
  isDirty,
  lastSaved,
}: EntityEditActionsProps) {
  const t = useTranslations();
  const format = useFormatContext();
  return (
    <div className="flex shrink-0 items-center gap-3 border-t border-border bg-background px-6 py-2.5 shadow-[0_-2px_12px_rgba(0,0,0,0.05)]">
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={onCancel}
        disabled={isSaving}
      >
        {t("cancel")}
      </Button>
      <Button
        type="submit"
        size="sm"
        disabled={isSaving || !isDirty}
        className="gap-1.5"
      >
        <HugeiconsIcon icon={FloppyDiskIcon} size={14} />
        {t("save")}
      </Button>
      {saveOption}
      <div className="flex-1" />
      {lastSaved && (
        <span className="text-xs text-muted-foreground">
          {t("entityEdit.lastSaved", {
            time: formatTimestampMinutes(lastSaved, format),
          })}
        </span>
      )}
      {deleteAction}
    </div>
  );
}
