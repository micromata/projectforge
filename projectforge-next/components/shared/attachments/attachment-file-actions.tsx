"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Delete01Icon,
  Download01Icon,
  ViewIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  attachmentDownloadUrl,
  type AttachmentRef,
} from "@/lib/rs/attachments";

/**
 * Delete, download and preview, for the details of one file (see AttachmentEditDialog) — the left half of its
 * footer, as the legacy dialog had them beside "Update".
 */
export function AttachmentFileActions({
  fileRef,
  busy,
  onDelete,
  onView,
}: {
  fileRef: AttachmentRef;
  busy?: boolean;
  /**
   * Only opens the question; the caller asks before deleting (see AttachmentFiles). Without it there is
   * no delete button — read-only details.
   */
  onDelete?: () => void;
  /** Opens the file in AttachmentViewer; without it (a file the browser can't show) there is no preview button. */
  onView?: () => void;
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-col-reverse gap-2 sm:flex-row">
      {onDelete && (
        <Button
          type="button"
          variant="destructive"
          disabled={busy}
          onClick={onDelete}
        >
          <HugeiconsIcon icon={Delete01Icon} size={16} />
          {t("delete")}
        </Button>
      )}
      {/* A link, not a fetch: the answer is the file, which the browser has to save itself. */}
      <Button type="button" variant="outline" asChild>
        <a href={attachmentDownloadUrl(fileRef)}>
          <HugeiconsIcon icon={Download01Icon} size={16} />
          {t("download._")}
        </a>
      </Button>
      {onView && (
        <Button type="button" variant="outline" onClick={onView}>
          <HugeiconsIcon icon={ViewIcon} size={16} />
          {t("preview")}
        </Button>
      )}
    </div>
  );
}
