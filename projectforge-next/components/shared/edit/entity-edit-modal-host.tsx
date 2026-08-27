"use client";

import { useEntityEditModalStore } from "@/store/entity-edit-modal-store";
import { EntityEditModal } from "./entity-edit-modal";

/**
 * Renders the one edit modal the store holds open, mounted once at the top of the authenticated tree
 * so anything below — the calendar, a list, the structure wizard — can open an entity's edit form in a
 * dialog with `openEntityEdit(...)` instead of navigating (see useEntityEditModalStore).
 */
export function EntityEditModalHost() {
  const descriptor = useEntityEditModalStore((s) => s.descriptor);
  const closeEntityEdit = useEntityEditModalStore((s) => s.closeEntityEdit);

  if (!descriptor) return null;

  return (
    <EntityEditModal
      // A fresh mount per entry, so opening a second one never reuses the first one's form state.
      key={`${descriptor.page.entity}:${descriptor.id ?? "new"}`}
      page={descriptor.page}
      id={descriptor.id}
      newParams={descriptor.newParams}
      prefill={descriptor.prefill}
      dirtyPrefill={descriptor.dirtyPrefill}
      open
      onOpenChange={(next) => {
        if (!next) closeEntityEdit();
      }}
      onSaved={descriptor.onSaved}
      onClose={descriptor.onClose}
    />
  );
}
