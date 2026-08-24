"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete01Icon, Edit02Icon, LockIcon } from "@hugeicons/core-free-icons";
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
  /** Nothing but downloads: no rename, no delete — and a row click downloads (see below). */
  readOnly?: boolean;
  /** Omitted where a selection makes no sense (read-only list): the checkbox then stays away. */
  selected?: boolean;
  onSelectedChange?: (selected: boolean) => void;
}

/**
 * One attachment: name, description and metadata, plus rename and delete.
 *
 * Nothing is formatted here — `sizeHumanReadable`, `lastUpdateFormatted` and `lastUpdateTimeAgo`
 * arrive ready from the backend, in the user's locale and timezone (see Attachment).
 *
 * The name itself is the download link, and the rest of the row opens the details — the section is
 * wide, so buttons pinned to its right edge are a long way from the file one is looking at. Download
 * is a plain link, not a fetch: the answer is the file itself, so the browser has to handle it (see
 * attachmentDownloadUrl).
 *
 * A row without a rename downloads on a click instead, like the legacy list (`DynamicAttachmentList`'s
 * `handleRowClick`): there is no detail dialog to reach when nothing in it can be changed. That is every
 * read-only row, and the one whose description is a marker rather than a text — that one keeps its
 * delete, since deleting it is the same call with the same bookkeeping as for any other file
 * (`AttachmentsService.deleteAttachment` writes the counters back either way).
 */
export function AttachmentRow({
  attachment,
  entity,
  id,
  onEdit,
  onDelete,
  busy,
  readOnly,
  selected,
  onSelectedChange,
}: Props) {
  const t = useTranslations();
  // Either the whole list is read-only, or this one file is (`Attachment.readonly`).
  const readonly = readOnly === true || attachment.readonly === true;
  // A file whose description carries a marker can be deleted but not renamed (`Attachment.renameLocked`),
  // so the pencil and the row's click follow this one and the trash follows `readonly`.
  const noRename = readonly || attachment.renameLocked === true;
  const downloadUrl = attachmentDownloadUrl({
    entity,
    id,
    fileId: attachment.fileId,
  });
  const downloadLabel = `${t("download._")}: ${attachment.name}`;

  return (
    <li className="relative flex items-center gap-3 border-b border-border/60 py-2 last:border-b-0 hover:bg-muted/40 last:hover:rounded-b-md">
      {/* The row's own click, as an element covering it rather than an onClick on the <li>, with the
          real controls layered above it (`z-10`). A mouse shortcut only: it duplicates the name link
          resp. the pencil, so it stays out of the tab order and out of the accessibility tree —
          otherwise every row would answer to two identical names. */}
      {noRename ? (
        <a
          href={downloadUrl}
          className="absolute inset-0"
          tabIndex={-1}
          aria-hidden
        />
      ) : (
        <button
          type="button"
          className="absolute inset-0"
          disabled={busy}
          tabIndex={-1}
          aria-hidden
          onClick={() => onEdit(attachment)}
        />
      )}
      {onSelectedChange && (
        <Checkbox
          className="relative z-10"
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
          {/* The name is the download: the shortest way to the file, and the only one a keyboard or
              screen reader gets — the row overlay above is for the mouse. */}
          <a
            href={downloadUrl}
            aria-label={downloadLabel}
            className="relative z-10 truncate text-xs font-medium hover:underline"
          >
            {attachment.name}
          </a>
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
      {!readonly && (
        <div className="relative z-10 flex shrink-0 items-center gap-0.5">
          {!noRename && (
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
          )}
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
        </div>
      )}
    </li>
  );
}
