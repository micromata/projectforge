"use client";

import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { EntityWithId } from "@/hooks/use-entity-detail";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { EditablePageDef } from "@/lib/page-def/types";
import { EntityEditDialogForm } from "./entity-edit-dialog-form";

export interface EntityEditDialogProps<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
> {
  page: EditablePageDef<Row, Values, Data, M>;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /**
   * Values written over the backend's preset, e.g. `{ name: "Project X-pm" }`.
   *
   * Compared by content, so a caller may build the object inline: a new object of the same values
   * must not re-seed the form and throw away what the user has typed.
   */
  prefill?: Partial<Values>;
  /** The entry was created — `id` as the backend assigned it, `values` as they were saved. */
  onSaved: (id: number | null, values: Values) => void;
}

/**
 * Adds an entry in a dialog, with the entity's own hand built form — the sibling of
 * [EntityEditPage] for a caller that needs something created *without leaving what it is doing*: the
 * structure wizard needs a group, and sending the user to the group page would abandon the wizard's
 * other choices (see WizardGroupStepCard).
 *
 * Deliberately less than the page: no routing, no tabs, no header with the way back, no legacy link,
 * no delete and no clone. What is left is the form itself, which is the only part that makes sense
 * inside a dialog — everything omitted is about *being* a page.
 *
 * Adding only: an existing entry is edited on its own page, where its history and attachments are.
 */
export function EntityEditDialog<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({
  page,
  open,
  onOpenChange,
  prefill,
  onSaved,
}: EntityEditDialogProps<Row, Values, Data, M>) {
  const t = useTranslations();
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[85vh] w-[95vw] gap-0 overflow-hidden !max-w-3xl p-0">
        <DialogHeader className="border-b px-6 py-4">
          {/* The title of the entity's own add page, so the dialog is named the way that page is. */}
          <DialogTitle>{t(page.edit.newTitleKey)}</DialogTitle>
        </DialogHeader>
        {/* Mounted only while it is open, and unmounted with it: the form starts from the backend's
            preset every time it is opened, and one filled in once must not come back on the next. */}
        {open && (
          <EntityEditDialogForm
            page={page}
            onOpenChange={onOpenChange}
            prefill={prefill}
            onSaved={onSaved}
          />
        )}
      </DialogContent>
    </Dialog>
  );
}
