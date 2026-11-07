"use client";

import { AttachmentUploadRow } from "@/components/shared/attachments/attachment-upload-row";
import type { UploadProgress } from "@/lib/rs/upload";
import type { PendingFile } from "./types";

/**
 * The files of a message on their way: one bar for all of them, as they go in one request with the message — the
 * bar of the data transfer's uploads, with its percentage, rate and cancel button.
 */
export function SendingFiles({
  files,
  progress,
  onCancel,
}: {
  files: PendingFile[];
  /** Null until the first progress event: the bar waits ("…") rather than showing 0 %. */
  progress: UploadProgress | null;
  onCancel: () => void;
}) {
  return (
    <ul className="px-2">
      <AttachmentUploadRow
        job={{
          id: 0,
          name: files.map((it) => it.file.name).join(", "),
          size: files.reduce((sum, it) => sum + it.file.size, 0),
          percent: progress?.percent ?? null,
          bytesPerSecond: progress?.bytesPerSecond ?? null,
          state: "sending",
        }}
        onCancel={onCancel}
      />
    </ul>
  );
}
