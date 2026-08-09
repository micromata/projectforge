"use client";

import { useCallback } from "react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Spinner } from "@/components/shared/spinner";
import {
  useAttachments,
  useAttachmentMutations,
} from "@/hooks/use-attachments";
import { useAttachmentUploads } from "@/hooks/use-attachment-uploads";
import type { AttachmentWriteResult } from "@/lib/rs/attachments";
import { AttachmentDropArea } from "./attachment-drop-area";
import { AttachmentFiles } from "./attachment-files";
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
export function AttachmentList({ entity, id, readOnly }: AttachmentListProps) {
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

  return (
    <div className="flex flex-col gap-3">
      {!readOnly && <AttachmentDropArea onFiles={uploads.enqueue} />}
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
        // Nothing stored and nothing on its way: the uploads above would otherwise be contradicted.
        uploads.jobs.length === 0 && (
          <p className="text-xs text-muted-foreground">{t("nothingFound")}</p>
        )
      ) : (
        <AttachmentFiles
          attachments={attachments}
          entity={entity}
          id={id}
          readOnly={readOnly}
        />
      )}
    </div>
  );
}
