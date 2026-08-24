"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { toast } from "@/lib/toast";
import { Spinner } from "@/components/shared/spinner";
import {
  useAttachments,
  useAttachmentMutations,
} from "@/hooks/use-attachments";
import { useAttachmentUploads } from "@/hooks/use-attachment-uploads";
import type { AttachmentWriteResult } from "@/lib/rs/attachments";
import { AttachmentDropArea } from "./attachment-drop-area";
import { AttachmentDropZone } from "./attachment-drop-zone";
import { AttachmentFiles } from "./attachment-files";
import { AttachmentUploadRow } from "./attachment-upload-row";

export interface AttachmentListProps {
  /** Rest path of the entity, i.e. `AbstractPagesRest.category` — "book", "contract", … */
  entity: string;
  /** null for an entity that isn't saved yet: the JCR needs a persisted id to attach to. */
  id: number | null;
  /** Only downloads are offered — for a user without write access to the entity. */
  readOnly?: boolean;
  /**
   * Compact variant for a section between the fields of a form (books, orders, …): no permanent drop
   * box, the click sits as a button in the toolbar and the whole section takes a drop.
   *
   * Left off where the attachments are the page (a standalone transfer area): there the big dashed
   * box is the point — it says what the page is for and gives the drop a target one can aim at.
   */
  embedded?: boolean;
  /**
   * Shows the attachment carrying exactly this description as a row like any other — downloadable and
   * deletable — except that it cannot be renamed.
   *
   * For a file the backend stores as an attachment but treats as a value of its own: the invoice PDF is
   * marked by the description `__INVOICE_PDF__` (`EInvoiceExportService.INVOICE_PDF_MARKER`) and has a
   * field of its own. Listed here nonetheless, because it is one of the files of this entity and reaching
   * it with one click is the point of the list.
   *
   * Only the rename is withheld, and not because the file is precious: the dialog edits the description,
   * which here *is* the marker — and since the row shows `lockedLabel` in its place, saving that dialog
   * would replace the marker with the label and silently turn the invoice PDF into an ordinary
   * attachment. Deleting on the other hand is the very same call as for its neighbours
   * (`AttachmentsService.deleteAttachment`, counters written back included), so withholding it would only
   * be a detour to the button in the file's own field.
   *
   * Client side because the marker is a backend convention that no endpoint filters by: `getAttachments`
   * answers the whole node, and `AbstractEntityRest.getById` — where the list is read from — is not
   * overridable. Which keeps this component free of any invoice knowledge: all it is told is which
   * description marks the locked file and what to call it.
   */
  lockedDescription?: string;
  /**
   * What the locked row shows instead of its description — the name of the role the file plays ("invoice
   * PDF"), since the marker itself is an internal string no user should be shown.
   */
  lockedLabel?: string;
  /**
   * Called after the stored files changed here (upload, rename, delete).
   *
   * For a page that shows one of these files a second time, as a value of its own: the invoice PDF's field
   * keeps a query of its own, which would otherwise still offer a file this list has just deleted. The
   * list itself refreshes without it (see useAttachmentMutations).
   */
  onChanged?: () => void;
}

/**
 * The attachments of one entity: upload, download, rename, delete, and the same on a whole
 * selection (download as one ZIP, delete at once — see AttachmentFiles).
 *
 * Generic, not tied to a feature: every `AbstractPagesRest` page may have attachments (books,
 * contracts, orders, invoices, scripts), so this is the shared counterpart of the legacy
 * `UIAttachmentList` and its `DynamicAttachmentList.jsx`.
 *
 * Not a `DataTable`: a handful of files per entity inside a form section have no sorting, paging or
 * column state to speak of, and the table primitive's own scroll container would fight the form's.
 * The selection is a set of `fileId`s of its own (see useAttachmentSelection) rather than the
 * table's index-keyed row selection.
 */
export function AttachmentList({
  entity,
  id,
  readOnly,
  embedded,
  lockedDescription,
  lockedLabel,
  onChanged,
}: AttachmentListProps) {
  const t = useTranslations();
  const { data, isLoading, isError } = useAttachments(entity, id);
  const { mergeResult } = useAttachmentMutations(entity, id);

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
      } else {
        onChanged?.();
      }
    },
    [mergeResult, onChanged, t]
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

  // `renameLocked` is a flag the row acts on by itself, so the locked file needs no case of its own
  // downstream: it keeps its download and its delete and loses the pencil.
  const attachments = (data ?? []).map((a) =>
    lockedDescription && a.description === lockedDescription
      ? { ...a, renameLocked: true, description: lockedLabel }
      : a
  );
  // Embedded, the toolbar carries the add button, so it has to be there before the first file too.
  const showFiles = attachments.length > 0 || (embedded && !readOnly);

  const content = (
    <div className="flex flex-col gap-3">
      {!embedded && !readOnly && (
        <AttachmentDropArea onFiles={uploads.enqueue} />
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
      {showFiles && (
        <AttachmentFiles
          attachments={attachments}
          entity={entity}
          id={id}
          readOnly={readOnly}
          onFiles={embedded ? uploads.enqueue : undefined}
          onChanged={onChanged}
        />
      )}
      {/* Nothing stored and nothing on its way: the uploads above would otherwise be contradicted. */}
      {attachments.length === 0 && uploads.jobs.length === 0 && (
        <p className="text-xs text-muted-foreground">{t("nothingFound")}</p>
      )}
    </div>
  );

  // Embedded there is no dashed box to aim at, so the whole section takes the drop instead.
  return embedded && !readOnly ? (
    <AttachmentDropZone onFiles={uploads.enqueue}>{content}</AttachmentDropZone>
  ) : (
    content
  );
}
