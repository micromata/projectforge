"use client";

import type { ReactNode } from "react";
import { EntityCloneButton } from "./entity-clone-button";
import { EntityConvertButton } from "./entity-convert-button";
import { EntityDeleteButton } from "./entity-delete-button";
import { EntityForceDeleteButton } from "./entity-force-delete-button";
import { EntityEditActions } from "./entity-edit-actions";
import { EntityUndeleteButton } from "./entity-undelete-button";

export interface EntityEditActionBarProps {
  onCancel: () => void;
  saveOption?: ReactNode;
  canSave: boolean;
  isSaving: boolean;
  isDirty: boolean;
  /** Whether an unchanged form may still be saved — true for a new entry (see EntityEditActions). */
  allowSaveUnchanged: boolean;
  lastSaved: string | null;
  /** Clone, delete and undelete each appear only where the entry and this user's access allow it. */
  showClone: boolean;
  showDelete: boolean;
  showUndelete: boolean;
  /** The irrevocable delete, where the entity allows it (see EditDef.forceDelete) and the entry exists. */
  showForceDelete: boolean;
  /** The conversion, where the entity declares one (see EditDef.convert); its label varies by direction. */
  showConvert: boolean;
  convertLabel: string;
  onClone: () => void | Promise<void>;
  onDelete: () => void | Promise<void>;
  onForceDelete: () => void | Promise<void>;
  onUndelete: () => void | Promise<void>;
  onConvert: () => void | Promise<void>;
  cloneDisabled: boolean;
  deleteDisabled: boolean;
  forceDeleteDisabled: boolean;
  undeleteDisabled: boolean;
  convertDisabled: boolean;
}

/**
 * The save/cancel bar with the entry's clone, delete and undelete buttons — the part of the edit form
 * that is the same whether the form is on its own page or in a modal (see EntityEditBody). Which of
 * the three optional buttons show is decided by the `show*` flags the body computes from the entry and
 * the user's access.
 */
export function EntityEditActionBar({
  onCancel,
  saveOption,
  canSave,
  isSaving,
  isDirty,
  allowSaveUnchanged,
  lastSaved,
  showClone,
  showDelete,
  showUndelete,
  showForceDelete,
  showConvert,
  convertLabel,
  onClone,
  onDelete,
  onForceDelete,
  onUndelete,
  onConvert,
  cloneDisabled,
  deleteDisabled,
  forceDeleteDisabled,
  undeleteDisabled,
  convertDisabled,
}: EntityEditActionBarProps) {
  return (
    <EntityEditActions
      onCancel={onCancel}
      saveOption={saveOption}
      cloneAction={
        showClone ? (
          <EntityCloneButton onClone={onClone} disabled={cloneDisabled} />
        ) : undefined
      }
      convertAction={
        showConvert ? (
          <EntityConvertButton
            label={convertLabel}
            onConvert={onConvert}
            disabled={convertDisabled}
          />
        ) : undefined
      }
      forceDeleteAction={
        showForceDelete ? (
          <EntityForceDeleteButton
            onForceDelete={onForceDelete}
            disabled={forceDeleteDisabled}
          />
        ) : undefined
      }
      deleteAction={
        showDelete ? (
          <EntityDeleteButton onDelete={onDelete} disabled={deleteDisabled} />
        ) : undefined
      }
      undeleteAction={
        showUndelete ? (
          <EntityUndeleteButton
            onUndelete={onUndelete}
            disabled={undeleteDisabled}
          />
        ) : undefined
      }
      canSave={canSave}
      isSaving={isSaving}
      isDirty={isDirty}
      allowSaveUnchanged={allowSaveUnchanged}
      lastSaved={lastSaved}
    />
  );
}
