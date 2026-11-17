"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ViewIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { previewKind, type Attachment } from "@/lib/rs/attachments";
import { useAttachmentService } from "./attachment-service";

/**
 * Shows the file in AttachmentViewer, as an eye in the action color next to its name in the list and in the table —
 * set apart from the name, which is the download, with the tooltip saying what it does. Nothing for a file the browser can't show ([previewKind]) or a
 * backend without the preview: the name stays the download.
 */
export function AttachmentViewButton({
  attachment,
  onView,
}: {
  attachment: Attachment;
  onView?: (attachment: Attachment) => void;
}) {
  const t = useTranslations();
  const service = useAttachmentService();
  if (!onView || !service.preview || !previewKind(attachment)) return null;
  return (
    // No openOnTap: the pill is an action itself, a tap has to open the preview.
    <HintTooltip text={t("attachment.view.tooltip")}>
      <Button
        type="button"
        variant="ghost"
        size="icon-sm"
        // Above the row's own click overlay (see AttachmentRow).
        className="relative z-10 shrink-0 text-primary hover:text-primary"
        aria-label={`${t("preview")}: ${attachment.name}`}
        onClick={(event) => {
          // The table row's click would open the details as well.
          event.stopPropagation();
          onView(attachment);
        }}
      >
        <HugeiconsIcon icon={ViewIcon} className="size-4.5" />
      </Button>
    </HintTooltip>
  );
}
