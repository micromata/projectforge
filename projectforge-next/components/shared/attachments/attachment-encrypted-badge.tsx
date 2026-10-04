"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { LockIcon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { zipModeMessageKey, type Attachment } from "@/lib/rs/attachments";

/**
 * Marks an encrypted attachment in the list: lock and "verschlüsselt", in the info tone.
 *
 * A badge rather than a bare icon: a file that only opens with its password must be told apart at a
 * glance — whoever downloads it without knowing gets a ZIP they cannot open. The tooltip names how it
 * is encrypted (`zipMode`); a file kept AES-encrypted in the storage has no zipMode, and then the plain
 * "verschlüsselt" is all there is to say.
 */
export function AttachmentEncryptedBadge({
  attachment,
}: {
  attachment: Attachment;
}) {
  const t = useTranslations();
  const mode = zipModeMessageKey(attachment.zipMode);
  return (
    <HintTooltip text={mode ? t(mode) : null} openOnTap>
      <Badge
        variant="outline"
        // Above the row's own click overlay (see AttachmentRow), so the tooltip can open.
        className="relative z-10 h-4 shrink-0 gap-0.5 border-status-info-border bg-status-info-bg px-1 text-[10px] font-medium text-status-info"
      >
        <HugeiconsIcon icon={LockIcon} size={10} strokeWidth={2} />
        {t("attachment.zip.encrypted")}
      </Badge>
    </HintTooltip>
  );
}
