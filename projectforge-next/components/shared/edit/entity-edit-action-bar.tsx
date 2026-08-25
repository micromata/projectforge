"use client";

import type { ReactNode } from "react";
import { EntityCloneButton } from "./entity-clone-button";
import { EntityDeleteButton } from "./entity-delete-button";
import { EntityEditActions } from "./entity-edit-actions";
import { EntityUndeleteButton } from "./entity-undelete-button";

export interface EntityEditActionBarProps {
  onCancel: () => void;
  saveOption?: ReactNode;
  canSave: boolean;
  isSaving: boolean;
  isDirty: boolean;
  lastSaved: string | null;
  /** Clone, delete and undelete each appear only where the entry and this user's access allow it. */
  showClone: boolean;
  showDelete: boolean;
  showUndelete: boolean;
  onClone: () => void | Promise<void>;
  onDelete: () => void | Promise<void>;
  onUndelete: () => void | Promise<void>;
  cloneDisabled: boolean;
  deleteDisabled: boolean;
  undeleteDisabled: boolean;
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
  lastSaved,
  showClone,
  showDelete,
  showUndelete,
  onClone,
  onDelete,
  onUndelete,
  cloneDisabled,
  deleteDisabled,
  undeleteDisabled,
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
      lastSaved={lastSaved}
    />
  );
}
