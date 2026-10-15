import { HugeiconsIcon } from "@hugeicons/react";
import { Attachment01Icon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";

interface AttachmentsSummaryProps {
  /** Number of attachments; absent or 0 for an entity without any. */
  count?: number | null;
  /**
   * Count and total size in one, as the backend formatted it in the user's locale
   * ("5,2MB (3)"). Taken as it is — reformatting here would be a second place to be wrong.
   * @see org.projectforge.framework.jcr.AttachmentsInfo.getAttachmentsSizeFormatted
   */
  formatted?: string | null;
  /** Accessible name, e.g. "3 attachments, 5,2MB". The visible text is the icon's label. */
  label: string;
  className?: string;
}

/**
 * How many attachments an entity has and how much they weigh, for a list row. Nothing at all
 * without attachments: a "-" in every row of a mostly empty column is noise.
 */
export function AttachmentsSummary({
  count,
  formatted,
  label,
  className,
}: AttachmentsSummaryProps) {
  if (!count) return null;
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 text-muted-foreground",
        className
      )}
      aria-label={label}
      role="img"
    >
      <HugeiconsIcon icon={Attachment01Icon} size={13} aria-hidden />
      <span className="tabular-nums">{formatted}</span>
    </span>
  );
}
