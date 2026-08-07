"use client";

import { useCallback } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  deleteAttachment,
  fetchAttachments,
  modifyAttachment,
  type Attachment,
  type AttachmentWriteResult,
} from "@/lib/rs/attachments";

/** Query key of an entity's attachments. */
export function attachmentsQueryKey(entity: string, id: number | null) {
  return ["attachments", entity, id] as const;
}

/**
 * The attachments of one entity. Generic on purpose: every `AbstractPagesRest` page can have them,
 * so nothing here belongs to a feature.
 *
 * @param id null or 0 for an unsaved entity — there is nothing to attach to yet, so the query stays
 * disabled (the backend needs a persisted id, see `attachment.onlyAvailableAfterSave`).
 */
export function useAttachments(entity: string, id: number | null) {
  return useQuery<Attachment[]>({
    queryKey: attachmentsQueryKey(entity, id),
    queryFn: ({ signal }) => fetchAttachments(entity, id!, signal),
    enabled: id != null && Number.isFinite(id) && id > 0,
  });
}

/**
 * Rename and delete, each writing the answer straight into the cache: both endpoints return the
 * entity's complete new list (see lib/rs/attachments.ts), so there is nothing to re-read.
 *
 * Uploading has a hook of its own (see use-attachment-uploads.ts): it tracks one progress per file
 * and runs the files in parallel, which is what [mergeResult] exists for.
 *
 * A refused write (`kind: "rejected"`, e.g. a duplicate filename) leaves the cache alone — the list
 * on screen is still the truth. The caller shows the backend's message.
 */
export function useAttachmentMutations(entity: string, id: number | null) {
  const qc = useQueryClient();
  const key = attachmentsQueryKey(entity, id);

  const applyResult = useCallback(
    (result: AttachmentWriteResult): AttachmentWriteResult => {
      if (result.kind === "ok") qc.setQueryData(key, result.attachments);
      return result;
    },
    // The key is an array literal, so it is a new object each render — its contents are what matter.
    [qc, entity, id] // eslint-disable-line react-hooks/exhaustive-deps
  );

  /**
   * Like [applyResult], but for an answer that may be out of date already.
   *
   * Uploads run in parallel (see use-attachment-uploads.ts), so their answers can arrive in any
   * order while each carries a full snapshot of the list. Replacing the cache with a snapshot taken
   * before a sibling finished would make that sibling disappear from the screen, even though it is
   * stored. Merging by `fileId` keeps every file either side knows about; the sequence of `modify`
   * and `delete` is unaffected, as those go through the mutations below.
   */
  const mergeResult = useCallback(
    (result: AttachmentWriteResult): AttachmentWriteResult => {
      if (result.kind !== "ok") return result;
      qc.setQueryData<Attachment[]>(key, (current) => {
        if (!current?.length) return result.attachments;
        const merged = [...current];
        for (const attachment of result.attachments) {
          // Per file the answer wins, since it is the server's own state of it; a file it doesn't
          // mention stays, because a parallel sibling may have added it after this snapshot.
          const at = merged.findIndex((a) => a.fileId === attachment.fileId);
          if (at >= 0) merged[at] = attachment;
          else merged.push(attachment);
        }
        return merged;
      });
      return result;
    },
    [qc, entity, id] // eslint-disable-line react-hooks/exhaustive-deps
  );

  const rename = useMutation<
    AttachmentWriteResult,
    Error,
    { fileId: string; name: string; description: string }
  >({
    mutationFn: ({ fileId, name, description }) =>
      modifyAttachment({ entity, id: id!, fileId }, name, description),
    onSuccess: applyResult,
  });

  const remove = useMutation<AttachmentWriteResult, Error, string>({
    mutationFn: (fileId) => deleteAttachment({ entity, id: id!, fileId }),
    onSuccess: applyResult,
  });

  return { rename, remove, mergeResult };
}
