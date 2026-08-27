import { create } from "zustand";
import type { NewEntryParams } from "@/hooks/use-entity-detail";
import type { EditablePageDef } from "@/lib/page-def/types";

/**
 * Any entity's edit declaration — the descriptor carries the page def object itself, so the modal is
 * entity-agnostic without a registry (the calendar and the wizard already import the def they open).
 *
 * `any` across the four type parameters on purpose: the store holds defs of *different* entities, whose
 * concrete metadata is used invariantly inside the def, so no single narrower bound accepts them all.
 * Each opener (the calendar, the wizard) passes a fully-typed def, and EntityEditModal re-infers them.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type AnyEditablePageDef = EditablePageDef<any, any, any, any>;

export interface EntityEditModalDescriptor {
  page: AnyEditablePageDef;
  /** null adds a new entry; a number edits that one. */
  id: number | null;
  /** What an "add" starts from — the calendar's break span, a team event's calendar. */
  newParams?: NewEntryParams;
  /** Values written over the preset — the wizard's "create group with this name". */
  prefill?: Record<string, unknown>;
  /**
   * Values applied over the loaded entry as a dirtying change — a calendar event opened on its
   * dragged/resized position, a move to persist (see calendar-edit-target).
   */
  dirtyPrefill?: Record<string, unknown>;
  /** The entry was saved. */
  onSaved?: (id: number | null, values: unknown) => void;
  /** The dialog closed without a save — e.g. refetch the calendar the entry belongs to. */
  onClose?: () => void;
}

interface EntityEditModalState {
  /** The one open edit modal, or null while none is. */
  descriptor: EntityEditModalDescriptor | null;
  openEntityEdit: (descriptor: EntityEditModalDescriptor) => void;
  closeEntityEdit: () => void;
}

/**
 * The single edit modal open across the app — global on purpose: the calendar opens a timesheet here
 * without navigating, and the descriptor has to outlive whatever raised it (see EntityEditModalHost).
 */
export const useEntityEditModalStore = create<EntityEditModalState>((set) => ({
  descriptor: null,
  openEntityEdit: (descriptor) => set({ descriptor }),
  closeEntityEdit: () => set({ descriptor: null }),
}));
