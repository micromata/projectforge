"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useAttachmentMutations } from "@/hooks/use-attachments";
import { useAttachmentSelection } from "@/hooks/use-attachment-selection";
import type { Attachment, AttachmentWriteResult } from "@/lib/rs/attachments";
import { AttachmentEditDialog } from "./attachment-edit-dialog";
import { AttachmentRow } from "./attachment-row";
import { AttachmentSelectionBar } from "./attachment-selection-bar";

interface Props {
  /** The stored attachments, in the order the backend returned them. */
  attachments: Attachment[];
  entity: string;
  id: number;
  /** Only downloads are offered — no selection, no rename, no delete. */
  readOnly?: boolean;
}

/**
 * The stored attachments of an entity: rename, delete, and the actions on a whole selection
 * (download as one ZIP, delete at once — see AttachmentSelectionBar).
 *
 * Split from AttachmentList so that one keeps to the uploads and the query while this one holds the
 * selection and both dialogs.
 */
export function AttachmentFiles({ attachments, entity, id, readOnly }: Props) {
  const t = useTranslations();
  const { rename, remove, removeMany } = useAttachmentMutations(entity, id);
  const selection = useAttachmentSelection(attachments);
  const [editing, setEditing] = useState<Attachment | null>(null);
  /** The files the open confirmation would delete — one row's, or a whole selection's. */
  const [deleting, setDeleting] = useState<Attachment[]>([]);

  const busy = rename.isPending || remove.isPending || removeMany.isPending;

  function report(result: AttachmentWriteResult): void {
    if (result.kind === "rejected") {
      toast.error(result.message || t("validation.error.generic"));
    }
  }

  async function saveEdit(name: string, description: string) {
    if (!editing) return;
    try {
      report(
        await rename.mutateAsync({ fileId: editing.fileId, name, description })
      );
      setEditing(null);
    } catch {
      toast.error(t("validation.error.generic"));
    }
  }

  async function confirmDelete() {
    const files = deleting;
    setDeleting([]);
    if (files.length === 0) return;
    try {
      // One call for a selection, so the cache is written once (see deleteAttachments).
      report(
        files.length === 1
          ? await remove.mutateAsync(files[0].fileId)
          : await removeMany.mutateAsync(files.map((file) => file.fileId))
      );
      // Whatever was deleted can no longer be picked; the rest of the selection stays.
      selection.clear();
    } catch {
      toast.error(t("validation.error.generic"));
    }
  }

  return (
    <>
      {!readOnly && (
        <AttachmentSelectionBar
          attachments={attachments}
          selection={selection}
          entity={entity}
          id={id}
          busy={busy}
          onDeleteSelected={setDeleting}
        />
      )}
      <ul className="flex flex-col">
        {attachments.map((attachment) => (
          <AttachmentRow
            key={attachment.fileId}
            attachment={attachment}
            entity={entity}
            id={id}
            busy={busy}
            readOnly={readOnly}
            selected={selection.has(attachment.fileId)}
            onSelectedChange={
              readOnly
                ? undefined
                : (on) => selection.toggle(attachment.fileId, on)
            }
            onEdit={setEditing}
            onDelete={(attachment) => setDeleting([attachment])}
          />
        ))}
      </ul>

      {editing && (
        <AttachmentEditDialog
          attachment={editing}
          saving={rename.isPending}
          onSave={(name, description) => void saveEdit(name, description)}
          onClose={() => setEditing(null)}
        />
      )}
      {deleting.length > 0 && (
        <ConfirmDialog
          open
          onOpenChange={(open) => !open && setDeleting([])}
          title={t("delete")}
          description={
            // The final question in both cases, not markAsDeletedQuestion: the JCR keeps no history
            // of removed files, so this cannot be undone.
            deleting.length === 1
              ? t("question.deleteQuestion")
              : t("file.upload.deleteSelected.confirm")
          }
          confirmLabel={t("delete")}
          destructive
          onConfirm={() => void confirmDelete()}
        />
      )}
    </>
  );
}
