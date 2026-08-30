"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FloppyDiskIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { useFormatContext } from "@/hooks/use-format";
import { useSubmitShortcutHint } from "@/hooks/use-submit-shortcut";
import { formatTimestampMinutes } from "@/lib/format";

export interface EntityEditActionsProps {
  onCancel: () => void;
  /**
   * Right of the save button — a choice about the save rather than about the entity (an order's
   * "send an e-mail notification?", see `EditDef.saveOption`).
   */
  saveOption?: ReactNode;
  /**
   * Clone button, left of the spacer with save and cancel: it acts on this entry rather than being a
   * choice about the save. Omitted for an entity that offers no clone, and while the entry doesn't
   * exist yet (see [EntityCloneButton]).
   */
  cloneAction?: ReactNode;
  /**
   * Convert button, beside [cloneAction]: like it, an action on this entry rather than a choice about
   * the save — it turns the entry into another entity (see [EntityConvertButton]). Omitted for an
   * entity that offers no conversion.
   */
  convertAction?: ReactNode;
  /**
   * Irrevocable-delete button, left of [deleteAction] and right-aligned with it. Omitted for an entity
   * that doesn't allow it, and while the entry doesn't exist yet (see [EntityForceDeleteButton]).
   */
  forceDeleteAction?: ReactNode;
  /**
   * Delete button, rendered right-aligned. Omitted while the entry doesn't exist yet — deleting it
   * needs the saved entity (see [EntityDeleteButton]).
   */
  deleteAction?: ReactNode;
  /**
   * Restore button, in the place of [deleteAction] — an entry is either deleted or it is not, so the
   * two are never here together (see EntityUndeleteButton).
   */
  undeleteAction?: ReactNode;
  /**
   * Whether this user may save at all (`writeAccess` of the entity). False leaves the save button and
   * its option out; the read-only form stays readable and cancel stays the way out.
   */
  canSave: boolean;
  isSaving: boolean;
  isDirty: boolean;
  /**
   * Whether an unchanged form may still be saved — true for a new entry (`id == null`). A clone or a
   * calendar-preset sheet arrives complete but pristine (its baseline *is* the preset), and creating it
   * as-is is a legitimate save, as Wicket's always-enabled create button is. An existing entry keeps the
   * dirty guard: there is nothing to write when nothing changed (see EntityEditBody).
   */
  allowSaveUnchanged: boolean;
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
  cloneAction,
  convertAction,
  forceDeleteAction,
  deleteAction,
  undeleteAction,
  canSave,
  isSaving,
  isDirty,
  allowSaveUnchanged,
  lastSaved,
}: EntityEditActionsProps) {
  const t = useTranslations();
  const format = useFormatContext();
  const shortcutHint = useSubmitShortcutHint();
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
      {/* Left out entirely without write access rather than disabled, as Wicket leaves it out
          (AbstractEditForm.updateButtonVisibility): a greyed-out save reads as "not yet", while there is
          nothing this user could do to enable it. Cancel stays — it is the way out. */}
      {canSave && (
        <>
          {/* The default button of the form, so its tooltip is where the keyboard shortcut is named
              (see useSubmitShortcut). */}
          <HintTooltip {...shortcutHint}>
            <Button
              type="submit"
              size="sm"
              disabled={isSaving || (!isDirty && !allowSaveUnchanged)}
              className="gap-1.5"
              // The button is the only thing that changes while saving, so it carries the busy state.
              aria-busy={isSaving}
            >
              {/* In place of the icon, not next to it, so the label doesn't move. A save can take seconds —
                  the order's notification mail is sent synchronously (AuftragDao.sendNotificationIfRequired)
                  and waits for the SMTP server — and a disabled button alone doesn't say that anything is
                  happening. */}
              {isSaving ? (
                <Spinner className="h-3.5 w-3.5 border-2" />
              ) : (
                <HugeiconsIcon icon={FloppyDiskIcon} size={14} />
              )}
              {t("save")}
            </Button>
          </HintTooltip>
          {saveOption}
        </>
      )}
      {cloneAction}
      {convertAction}
      <div className="flex-1" />
      {lastSaved && (
        <span className="text-xs text-muted-foreground">
          {t("entityEdit.lastSaved", {
            time: formatTimestampMinutes(lastSaved, format),
          })}
        </span>
      )}
      {forceDeleteAction}
      {deleteAction}
      {undeleteAction}
    </div>
  );
}
