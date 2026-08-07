"use client";

import { useCallback, useState } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { Spinner } from "@/components/shared/spinner";
import {
  useAttachments,
  useAttachmentMutations,
} from "@/hooks/use-attachments";
import { useAttachmentUploads } from "@/hooks/use-attachment-uploads";
import type { Attachment, AttachmentWriteResult } from "@/lib/rs/attachments";
import { AttachmentDropArea } from "./attachment-drop-area";
import { AttachmentEditDialog } from "./attachment-edit-dialog";
import { AttachmentRow } from "./attachment-row";
import { AttachmentUploadRow } from "./attachment-upload-row";

export interface AttachmentListProps {
  /** Rest path of the entity, i.e. `AbstractPagesRest.category` — "book", "contract", … */
  entity: string;
  /** null for an entity that isn't saved yet: the JCR needs a persisted id to attach to. */
  id: number | null;
  /** Only downloads are offered — for a user without write access to the entity. */
  readOnly?: boolean;
}

/**
 * The attachments of one entity: upload, download, rename, delete.
 *
 * Generic, not tied to a feature: every `AbstractPagesRest` page may have attachments (books,
 * contracts, orders, invoices, scripts), so this is the shared counterpart of the legacy
 * `UIAttachmentList` and its `DynamicAttachmentList.jsx`.
 *
 * Not a `DataTable`: a handful of files per entity inside a form section have no sorting, paging or
 * column state to speak of, and the table primitive's own scroll container would fight the form's.
 * Multi-select download/delete (`multiDownload`, `multiDelete`) is deliberately left out for the
 * same reason — the per-row actions cover the case without a selection model.
 */
export function AttachmentList({ entity, id, readOnly }: AttachmentListProps) {
  const t = useTranslations();
  const { data, isLoading, isError } = useAttachments(entity, id);
  const { rename, remove, mergeResult } = useAttachmentMutations(entity, id);
  const [editing, setEditing] = useState<Attachment | null>(null);
  const [deleting, setDeleting] = useState<Attachment | null>(null);

  /**
   * A refusal is a regular HTTP 200 answer (duplicate name, file too large) carrying the backend's
   * own translated text — showing that is more useful than any message of ours. For an upload the
   * text also stays on the file's own row, so it is clear which file was refused.
   */
  const onUploadResult = useCallback(
    (result: AttachmentWriteResult) => {
      // Merged, not replaced: uploads run in parallel, so this answer may already be missing a
      // sibling that finished after it (see useAttachmentMutations).
      mergeResult(result);
      if (result.kind === "rejected") {
        toast.error(result.message || t("file.upload.error._"));
      }
    },
    [mergeResult, t]
  );

  const uploads = useAttachmentUploads(entity, id, {
    onResult: onUploadResult,
    transferErrorMessage: t("file.upload.error._"),
  });

  // Nothing can be attached before the first save; the backend says so in its own words.
  if (id == null || id <= 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("attachment.onlyAvailableAfterSave")}
      </p>
    );
  }
  if (isLoading) {
    return <Spinner className="h-4 w-4 border-2" />;
  }
  if (isError) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("validation.error.generic")}
      </p>
    );
  }

  const attachments = data ?? [];
  const busy = rename.isPending || remove.isPending;

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
    if (!deleting) return;
    const file = deleting;
    setDeleting(null);
    try {
      report(await remove.mutateAsync(file.fileId));
    } catch {
      toast.error(t("validation.error.generic"));
    }
  }

  return (
    <div className="flex flex-col gap-3">
      {!readOnly && (
        <AttachmentDropArea onFiles={uploads.enqueue} disabled={busy} />
      )}
      {/* The uploads sit above the list: they are what just happened, and each finished one moves
          down into the list by itself. */}
      {uploads.jobs.length > 0 && (
        <ul className="flex flex-col">
          {uploads.jobs.map((job) => (
            <AttachmentUploadRow
              key={job.id}
              job={job}
              onCancel={uploads.cancel}
            />
          ))}
        </ul>
      )}
      {attachments.length === 0 ? (
        uploads.jobs.length === 0 && (
          <p className="text-xs text-muted-foreground">{t("nothingFound")}</p>
        )
      ) : (
        <ul className="flex flex-col">
          {attachments.map((attachment) => (
            <AttachmentRow
              key={attachment.fileId}
              attachment={attachment}
              entity={entity}
              id={id}
              busy={busy || readOnly}
              onEdit={setEditing}
              onDelete={setDeleting}
            />
          ))}
        </ul>
      )}

      {editing && (
        <AttachmentEditDialog
          attachment={editing}
          saving={rename.isPending}
          onSave={(name, description) => void saveEdit(name, description)}
          onClose={() => setEditing(null)}
        />
      )}
      {deleting && (
        <ConfirmDialog
          open
          onOpenChange={(open) => !open && setDeleting(null)}
          title={t("delete")}
          // The final question, not markAsDeletedQuestion: the JCR keeps no history of removed
          // files, so this cannot be undone.
          description={t("question.deleteQuestion")}
          confirmLabel={t("delete")}
          destructive
          onConfirm={() => void confirmDelete()}
        />
      )}
    </div>
  );
}
