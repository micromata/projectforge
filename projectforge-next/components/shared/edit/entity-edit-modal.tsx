"use client";

import { useCallback, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/shared/spinner";
import type { EntityWithId, NewEntryParams } from "@/hooks/use-entity-detail";
import { confirmLeaveUnsavedChanges } from "@/hooks/use-unsaved-changes-warning";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { EditablePageDef } from "@/lib/page-def/types";
import { EntityEditBody } from "./entity-edit-body";
import { EntityEditDialogShell } from "./entity-edit-dialog-shell";
import type { EditOutcome } from "./edit-outcome";
import type { ResponseAction } from "@/lib/rs/types";

export interface EntityEditModalProps<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  page: EditablePageDef<Row, Values, Data, M>;
  /** null adds a new entry; a number edits that one. */
  id: number | null;
  /** What an "add" starts from — the calendar's break span, a team event's calendar (see EditOutcome). */
  newParams?: NewEntryParams;
  /** Values written over the preset — the wizard's "create group with this name". */
  prefill?: Partial<Values>;
  /**
   * Values applied over the loaded entry as a dirtying change — a team event or a time sheet opened on
   * its dragged/resized position, which is a move to persist (see calendar-edit-target).
   */
  dirtyPrefill?: Partial<Values>;
  /** Tab beside the form to open on mount — the `?tab=` of a url that opened this modal. */
  initialTab?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /**
   * The entry was saved — `id` as the backend assigned it, `values` as they were saved, and the
   * write's `ResponseAction` (the calendar reads its `?gotoDate=…&hash=…` redirect url).
   */
  onSaved?: (
    id: number | null,
    values: unknown,
    action?: ResponseAction
  ) => void;
  /** The dialog closed without a save (cancel, delete, undelete, dismiss) — e.g. refetch the calendar. */
  onClose?: () => void;
  /**
   * Where a clone or a convert goes, when the host keeps it in place rather than letting it leave for
   * the target's own page. The calendar passes its own `/calendar`-prefixed push, so the prepared new
   * entry opens over the still-mounted calendar in this same dialog (see CalendarEditRouteClient); the
   * handover survives the navigation as a module variable (see usePendingClone). Omitted elsewhere (the
   * wizard), where a clone leaves for the entry's own page as before.
   */
  onCloneNavigate?: (route: string) => void;
}

/**
 * The edit form of an entity in a modal — the same [EntityEditBody] the page hosts, told that every
 * way it ends closes the dialog instead of navigating. The calendar opens a timesheet or a team event
 * here rather than on its own page, and the structure wizard adds a group without leaving its choices.
 *
 * A clone still leaves for the add page: a new entry built from this one belongs on its own page, not
 * layered in the dialog. Dismissing with unsaved changes asks first (`confirmLeaveUnsavedChanges`); the
 * deliberate ways out — save, cancel, delete — don't, being decisions the user just made.
 */
export function EntityEditModal<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({
  page,
  id,
  newParams,
  prefill,
  dirtyPrefill,
  initialTab,
  open,
  onOpenChange,
  onSaved,
  onClose,
  onCloneNavigate,
}: EntityEditModalProps<Row, Values, Data, M>) {
  const router = useRouter();
  const t = useTranslations();

  // A deliberate close from the form itself — never guarded, the decision was just made.
  const close = useCallback(() => onOpenChange(false), [onOpenChange]);

  const outcome: EditOutcome = useMemo(
    () => ({
      afterSave: (savedId, values, action) => {
        onSaved?.(savedId, values, action);
        close();
      },
      afterCancel: () => {
        onClose?.();
        close();
      },
      afterDelete: () => {
        onClose?.();
        close();
      },
      afterUndelete: () => {
        onClose?.();
        close();
      },
      afterClone: (route) => {
        // The host may keep the new entry in place (the calendar reopens it over itself); otherwise a
        // clone leaves for the target's own page, closing this dialog on the way out.
        if (onCloneNavigate) {
          onCloneNavigate(route);
          return;
        }
        close();
        router.push(route);
      },
    }),
    [onSaved, onClose, close, router, onCloneNavigate]
  );

  // ESC, the overlay and the close button all arrive here. Only these are guarded: a dirty form asks
  // before it is dismissed, where the form's own buttons above are deliberate and don't.
  const handleOpenChange = useCallback(
    (next: boolean) => {
      if (next) {
        onOpenChange(true);
        return;
      }
      // The dialog stays open while the app's own "leave anyway?" is answered; it closes only on "leave".
      void confirmLeaveUnsavedChanges().then((leave) => {
        if (leave) {
          onClose?.();
          onOpenChange(false);
        }
      });
    },
    [onOpenChange, onClose]
  );

  // The entry can't be shown (no read access, or it's gone) — close rather than sit empty. Nothing to
  // lose, so it isn't guarded.
  const handleUnavailable = useCallback(() => {
    onClose?.();
    onOpenChange(false);
  }, [onClose, onOpenChange]);

  // Known before the entry loads; names the dialog for the a11y label while the spinner shows.
  const loadingTitle = id == null ? t(page.edit.newTitleKey) : t(page.titleKey);

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className="flex max-h-[90vh] w-[95vw] flex-col gap-0 overflow-hidden p-0 !max-w-[95vw]"
        // Radix would focus the dialog itself; let useFocusFirstField place the caret in the first
        // field instead (see EntityEditBody).
        onOpenAutoFocus={(e) => e.preventDefault()}
        // A failure toast (a save refused for an overlapping time period is the case) is rendered
        // outside this content, in sonner's portal. Clicking or focusing it is an interaction "outside"
        // the dialog, which Radix would answer by closing it — dropping the very form the message is
        // about while the toast stays behind. Keep the dialog open for any interaction inside the
        // toaster; every other outside interaction still closes it through handleOpenChange.
        onInteractOutside={(event) => {
          const target = event.detail.originalEvent
            .target as HTMLElement | null;
          if (target?.closest("[data-sonner-toaster]")) event.preventDefault();
        }}
      >
        {/* Mounted only while open and unmounted with it: the form starts from the backend's preset
            every time it opens, and one filled in once must not come back on the next. */}
        {open && (
          <EntityEditBody
            page={page}
            id={id}
            newParams={newParams}
            prefill={prefill}
            dirtyPrefill={dirtyPrefill}
            outcome={outcome}
            formClassName="flex min-h-0 flex-1 flex-col overflow-hidden"
            renderLoading={() => (
              <>
                <DialogHeader className="sr-only">
                  <DialogTitle>{loadingTitle}</DialogTitle>
                </DialogHeader>
                <div className="flex items-center justify-center p-10">
                  <Spinner />
                </div>
              </>
            )}
            onUnavailable={handleUnavailable}
            renderShell={(regions) => (
              <EntityEditDialogShell
                regions={regions}
                initialTab={initialTab}
              />
            )}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
