"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import type { UseMutationResult } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import type { EntityForm } from "@/components/shared/form/form-context";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import { CLONE_PARAM, setPendingClone } from "@/hooks/use-pending-clone";
import type { EntityWriteResult } from "@/lib/rs/entity";
import {
  cloneAndSaveEntity,
  cloneEntity,
  convertEntity,
} from "@/lib/rs/entity";
import type { EditablePageDef } from "@/lib/page-def/types";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { EditOutcome } from "./edit-outcome";

type WriteMutation<Data extends EntityWithId> = UseMutationResult<
  EntityWriteResult,
  Error,
  Data
>;

export interface UseEditRunActionsOptions<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  page: EditablePageDef<Row, Values, Data, M>;
  id: number | null;
  /** The entity as the backend delivered it — what cancel, delete and undelete post back. */
  data: Data | undefined;
  form: EntityForm;
  cancelMutation: WriteMutation<Data>;
  deleteMutation: WriteMutation<Data>;
  forceDeleteMutation: WriteMutation<Data>;
  undeleteMutation: WriteMutation<Data>;
  outcome: EditOutcome;
}

/**
 * The four ways an edit form ends besides a save — cancel, delete, undelete and clone — as handlers
 * that do the work and then hand off to the injected [EditOutcome].
 *
 * Lifted out of [EntityEditPage] unchanged except for their tails: where the page navigated
 * (`router.push(back.route)`), each now calls the matching `outcome.after*`, so the page and the modal
 * differ only in what "afterwards" means (see EditOutcome). Save is not here — it runs through the
 * form's own submit (`useEntityEditForm`'s `onSaved`), which is the one seam that already existed.
 */
export function useEditRunActions<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({
  page,
  id,
  data,
  form,
  cancelMutation,
  deleteMutation,
  forceDeleteMutation,
  undeleteMutation,
  outcome,
}: UseEditRunActionsOptions<Row, Values, Data, M>) {
  const t = useTranslations();
  const [isCloning, setCloning] = useState(false);
  const [isConverting, setConverting] = useState(false);
  const [isForceDeleting, setForceDeleting] = useState(false);

  /**
   * Leaves without saving — and tells the backend so, which is what makes the list mark the entry the
   * user was looking at (`onCancelEdit`, same as after a save). Awaited, so the list is refetched with
   * the id already remembered; a cancel the server never answers must still leave, hence the caught
   * error. A new entry has no id to mark and nothing to report, so it skips the call.
   */
  async function runCancel(): Promise<void> {
    if (id != null && data) {
      await cancelMutation.mutateAsync(data).catch(() => undefined);
    }
    outcome.afterCancel();
  }

  async function runDelete(): Promise<void> {
    if (!data) return;
    const result = await deleteMutation.mutateAsync(data);
    if (result.kind === "validationErrors") {
      // Nothing was deleted; the server explains why (e.g. the entry is still referenced).
      result.validationErrors.forEach((error) => toast.error(error.message));
      return;
    }
    if (result.kind === "rejected") {
      // The delete was refused, not merely invalid — an AccessException (see lib/rs/entity.ts).
      toast.error(result.message || t("validation.error.generic"));
      return;
    }
    toast.success(t("message.successfullChanged"));
    outcome.afterDelete();
  }

  /**
   * Destroys the entry for good — row and history, no undo (`forceDeleteEntity`). The same tail as
   * [runDelete]: the entry is gone from the list either way, so afterwards means the same. Offered only
   * where the entity allows it and behind a confirmation (see EntityForceDeleteButton); the button
   * itself asks before this runs.
   */
  async function runForceDelete(): Promise<void> {
    if (!data) return;
    setForceDeleting(true);
    try {
      const result = await forceDeleteMutation.mutateAsync(data);
      if (result.kind === "validationErrors") {
        result.validationErrors.forEach((error) => toast.error(error.message));
        return;
      }
      if (result.kind === "rejected") {
        toast.error(result.message || t("validation.error.generic"));
        return;
      }
      toast.success(t("message.successfullChanged"));
      outcome.afterDelete();
    } finally {
      setForceDeleting(false);
    }
  }

  /**
   * Brings the entry back and leaves, the way a delete does — the list is where the user sees the entry
   * among the others again, which is what the restore was for.
   */
  async function runUndelete(): Promise<void> {
    if (!data) return;
    const result = await undeleteMutation.mutateAsync(data);
    if (result.kind === "validationErrors") {
      result.validationErrors.forEach((error) => toast.error(error.message));
      return;
    }
    if (result.kind === "rejected") {
      toast.error(result.message || t("validation.error.generic"));
      return;
    }
    toast.success(t("message.successfullChanged"));
    outcome.afterUndelete();
  }

  /**
   * The clone the button does, `CloneSupport.AUTOSAVE` (`page.edit.clone === "autosave"`): the copy is
   * saved straight away rather than opened as a new entry — a time sheet dragged onto another day and
   * cloned persists there without a detour through the add form (see cloneAndSaveEntity).
   *
   * On success the form ends the way a save does (`outcome.afterSave` — closing the calendar's dialog,
   * refetching, reactivating the user's own time sheets). When the backend can't save the copy — an
   * overlapping time period is the case, which cloning an *unmoved* sheet always is (the copy overlaps
   * the original), as is a busy target slot — it re-serves the prepared clone as an `UPDATE`. That is
   * the pre-AUTOSAVE clone: open it as a new unsaved entry for the user to adjust and save (see
   * runClone). The clone rides along from the answer (`variables.data`), so no second prepare call.
   */
  async function runCloneAndSave(): Promise<void> {
    setCloning(true);
    try {
      const result = await cloneAndSaveEntity<Data>(
        page.entity,
        form.state.values as unknown as Data
      );
      if (result.kind === "ok" && result.action.targetType !== "UPDATE") {
        // No collision: the copy was saved straight away. The write's ResponseAction rides along so the
        // calendar jumps to the clone's period the same way a plain save does (its `?gotoDate=…`).
        toast.success(t(page.edit.savedMessageKey));
        outcome.afterSave(result.id, form.state.values, result.action);
        return;
      }
      // Any validation error (an overlapping time period is the common one) means the copy wasn't
      // saved: fall back to the plain clone. The backend's `UPDATE` fall-back carries the prepared clone
      // under `variables.data`; a rejection without one is re-prepared unsaved via `cloneData` (see
      // cloneEntity).
      const prepared =
        (result.kind === "ok"
          ? (result.action.variables?.data as Data | undefined)
          : undefined) ??
        (await cloneEntity<Data>(
          page.entity,
          form.state.values as unknown as Data
        ));
      setPendingClone(page.entity, prepared);
      outcome.afterClone(`${page.route}/new?${CLONE_PARAM}=1`);
    } catch {
      // Nothing was written, so the form is still the way forward.
      toast.error(t("validation.error.generic"));
    } finally {
      setCloning(false);
    }
  }

  /**
   * Opens a new entry built from this one — Wicket's `RechnungEditPage.cloneData`.
   *
   * Posted are the *form's current values*, not the loaded entity, so unsaved edits travel; neither
   * side validates them (Wicket's `ignoreErrorOnClone` says the same), because a clone is a starting
   * point and not a write. The prepared clone is handed to the add page outside React (see
   * usePendingClone) — under `output: "export"` no state rides along a navigation.
   */
  async function runClone(): Promise<void> {
    if (page.edit.clone === "autosave") {
      await runCloneAndSave();
      return;
    }
    setCloning(true);
    try {
      const prepared = await cloneEntity<Data>(
        page.entity,
        form.state.values as unknown as Data
      );
      setPendingClone(page.entity, prepared);
      // The parameter is what tells the add page to take it (see usePendingClone) — and what keeps a
      // later plain `/new` a plain add, without the handover having to clear itself on the first read.
      outcome.afterClone(`${page.route}/new?${CLONE_PARAM}=1`);
    } catch {
      // Nothing was written, so the form is still the way forward.
      toast.error(t("validation.error.generic"));
    } finally {
      setCloning(false);
    }
  }

  /**
   * Turns this entry into an entry of another entity and opens its add page — a time sheet into a
   * calendar event and back (`EditDef.convert`, `switch2CalendarEvent` / `switch2Timesheet`).
   *
   * The same shape as [runClone], and it ends the same way: the prepared target is handed to *its* add
   * page (keyed by the target entity, so a time sheet's convert seeds the team event add page and not
   * this one) and the URL says a prepared entry waits there (`?clone=1`). Posted are the form's current
   * values, unvalidated — a conversion is a starting point, not a write (see convertEntity).
   */
  async function runConvert(): Promise<void> {
    const convert = page.edit.convert;
    if (!convert) return;
    setConverting(true);
    try {
      const prepared = await convertEntity(
        page.entity,
        convert.action,
        form.state.values as object
      );
      setPendingClone(convert.targetEntity, prepared);
      outcome.afterClone(`${convert.targetRoute}/new?${CLONE_PARAM}=1`);
    } catch {
      // Nothing was written, so the form is still the way forward.
      toast.error(t("validation.error.generic"));
    } finally {
      setConverting(false);
    }
  }

  return {
    runCancel,
    runDelete,
    runForceDelete,
    isForceDeleting,
    runUndelete,
    runClone,
    isCloning,
    runConvert,
    isConverting,
  };
}
