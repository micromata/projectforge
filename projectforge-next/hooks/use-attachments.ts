"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  deleteAttachment,
  fetchAttachments,
  modifyAttachment,
  uploadAttachment,
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
 * Upload, rename and delete, each writing the answer straight into the cache: every one of these
 * endpoints returns the entity's complete new list (see lib/rs/attachments.ts), so there is nothing
 * to re-read.
 *
 * A refused write (`kind: "rejected"`, e.g. a duplicate filename) leaves the cache alone — the list
 * on screen is still the truth. The caller shows the backend's message.
 */
export function useAttachmentMutations(entity: string, id: number | null) {
  const qc = useQueryClient();
  const key = attachmentsQueryKey(entity, id);

  function applyResult(result: AttachmentWriteResult): AttachmentWriteResult {
    if (result.kind === "ok") qc.setQueryData(key, result.attachments);
    return result;
  }

  const upload = useMutation<AttachmentWriteResult, Error, File>({
    mutationFn: (file) => uploadAttachment(entity, id!, file),
    onSuccess: applyResult,
  });

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

  return { upload, rename, remove };
}
