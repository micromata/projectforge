"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Delete01Icon,
  Download01Icon,
  Edit02Icon,
  LockIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { attachmentDownloadUrl, type Attachment } from "@/lib/rs/attachments";

interface Props {
  attachment: Attachment;
  entity: string;
  id: number;
  onEdit: (attachment: Attachment) => void;
  onDelete: (attachment: Attachment) => void;
  busy?: boolean;
  /** Omitted where a selection makes no sense (read-only list): the checkbox then stays away. */
  selected?: boolean;
  onSelectedChange?: (selected: boolean) => void;
}

/**
 * One attachment: name, description and metadata, plus download / rename / delete.
 *
 * Nothing is formatted here — `sizeHumanReadable`, `lastUpdateFormatted` and `lastUpdateTimeAgo`
 * arrive ready from the backend, in the user's locale and timezone (see Attachment).
 *
 * Download is a plain link, not a fetch: the answer is the file itself, so the browser has to
 * handle it (see attachmentDownloadUrl).
 */
export function AttachmentRow({
  attachment,
  entity,
  id,
  onEdit,
  onDelete,
  busy,
  selected,
  onSelectedChange,
}: Props) {
  const t = useTranslations();
  const readonly = attachment.readonly === true;

  return (
    <li className="flex items-center gap-3 border-b border-border/60 py-2 last:border-b-0">
      {onSelectedChange && (
        <Checkbox
          checked={selected ?? false}
          disabled={busy}
          // The rows all look alike, so the name has to say which file this picks.
          // `select._`, since the key has a `placeholder` subkey and so becomes a namespace.
          aria-label={`${t("select._")}: ${attachment.name}`}
          onCheckedChange={(checked) => onSelectedChange(checked === true)}
        />
      )}
      <div className="min-w-0 flex-1">
        <div className="flex items-baseline gap-1.5">
          <span className="truncate text-xs font-medium">
            {attachment.name}
          </span>
          {attachment.encrypted && (
            // Icon-only, so it needs a name of its own; the file can only be opened with its
            // password (Attachment.encrypted).
            <HugeiconsIcon
              icon={LockIcon}
              size={11}
              className="shrink-0 text-muted-foreground"
              aria-label={t("attachment.encryption")}
            />
          )}
        </div>
        {attachment.description && (
          <p className="truncate text-[11px] text-muted-foreground">
            {attachment.description}
          </p>
        )}
        <p className="text-[11px] text-muted-foreground">
          {[
            attachment.sizeHumanReadable,
            attachment.createdByUser,
            attachment.lastUpdateTimeAgo,
          ]
            .filter(Boolean)
            .join(" · ")}
        </p>
      </div>
      <div className="flex shrink-0 items-center gap-0.5">
        <Button
          asChild
          variant="ghost"
          size="icon"
          className="size-7 text-muted-foreground"
        >
          <a
            href={attachmentDownloadUrl({
              entity,
              id,
              fileId: attachment.fileId,
            })}
            // The name distinguishes the rows: every one of them has a download button.
            aria-label={`${t("download._")}: ${attachment.name}`}
          >
            <HugeiconsIcon icon={Download01Icon} size={13} />
          </a>
        </Button>
        {!readonly && (
          <>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-7 text-muted-foreground"
              disabled={busy}
              aria-label={`${t("edit")}: ${attachment.name}`}
              onClick={() => onEdit(attachment)}
            >
              <HugeiconsIcon icon={Edit02Icon} size={13} />
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-7 text-muted-foreground hover:text-destructive"
              disabled={busy}
              aria-label={`${t("delete")}: ${attachment.name}`}
              onClick={() => onDelete(attachment)}
            >
              <HugeiconsIcon icon={Delete01Icon} size={13} />
            </Button>
          </>
        )}
      </div>
    </li>
  );
}
