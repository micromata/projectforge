"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete01Icon, Download01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { attachmentsDownloadUrl, type Attachment } from "@/lib/rs/attachments";
import type { AttachmentSelection } from "@/hooks/use-attachment-selection";
import { AttachmentAddButton } from "./attachment-add-button";

interface Props {
  attachments: Attachment[];
  selection: AttachmentSelection;
  entity: string;
  id: number;
  /** Asks for the selected files to be deleted — the confirmation is the list's. */
  onDeleteSelected: (attachments: Attachment[]) => void;
  /** Picks files to attach; omitted for a read-only list, which then has no add button. */
  onFiles?: (files: File[]) => void;
  busy?: boolean;
}

/**
 * One line of actions above the files: add, select-all, the actions that work on a whole selection
 * (download as one ZIP, delete at once), and "download all" — which needs no selection at all.
 *
 * Adding lives here rather than in a drop area of its own, so the section stays as flat as the form
 * around it; dropping still works on the whole section (see AttachmentDropZone).
 *
 * The selection actions stay visible and merely disabled while nothing is picked, so it is
 * discoverable that a selection has actions at all — a bar that only appears once something is
 * selected leaves the checkboxes looking purposeless. With no file at all there is nothing to select,
 * so only the add button remains.
 *
 * Deleting a file the user may not touch is not offered: `Attachment.readonly` marks those, and the
 * backend would silently skip them (`multiDelete` filters by access), leaving a partial result that
 * looks like a failure.
 */
export function AttachmentToolbar({
  attachments,
  selection,
  entity,
  id,
  onDeleteSelected,
  onFiles,
  busy,
}: Props) {
  const t = useTranslations();
  const { selected } = selection;
  const allSelected =
    attachments.length > 0 && selected.length === attachments.length;
  const deletable = selected.filter((a) => a.readonly !== true);
  const fileIds = selected.map((a) => a.fileId);
  const allFileIds = attachments.map((a) => a.fileId);

  return (
    <div
      className={cn(
        "flex flex-wrap items-center gap-x-3 gap-y-2",
        attachments.length > 0 && "border-b border-border/60 pb-2"
      )}
    >
      {onFiles && <AttachmentAddButton onFiles={onFiles} disabled={busy} />}
      {attachments.length > 0 && (
        <>
          <div className="flex items-center gap-2">
            <Checkbox
              id={`${entity}-${id}-select-all`}
              checked={
                allSelected
                  ? true
                  : selected.length > 0
                    ? "indeterminate"
                    : false
              }
              disabled={busy}
              onCheckedChange={(checked) => selection.setAll(checked === true)}
            />
            <Label
              htmlFor={`${entity}-${id}-select-all`}
              className="text-[11px] font-normal text-muted-foreground"
            >
              {t("selectAll")}
              {selected.length > 0 && ` (${selected.length})`}
            </Label>
          </div>
          <div className="flex items-center gap-1">
            {/* Every file at once, without picking them first — the common case for a handful of
                files. No `downloadAll` endpoint on the generic attachments API, so the ZIP is asked
                for with the full list of ids (multiDownload with none means "empty", not "all"). */}
            <Button
              asChild
              variant="outline"
              size="sm"
              className="h-7 gap-1.5 text-[11px]"
            >
              <a href={attachmentsDownloadUrl(entity, id, allFileIds)}>
                <HugeiconsIcon icon={Download01Icon} size={13} />
                {t("attachment.downloadAll")}
              </a>
            </Button>
            {/* A link, not a fetch: the answer is the ZIP itself (see attachmentsDownloadUrl). With
                nothing picked it is an anchor without href — `disabled` means nothing on one, so it
                says so with `aria-disabled` and takes the button's own disabled look. */}
            <Button
              asChild
              variant="outline"
              size="sm"
              className="h-7 gap-1.5 text-[11px]"
            >
              <a
                href={
                  fileIds.length > 0
                    ? attachmentsDownloadUrl(entity, id, fileIds)
                    : undefined
                }
                aria-disabled={fileIds.length === 0}
                className={cn(
                  fileIds.length === 0 && "pointer-events-none opacity-50"
                )}
              >
                <HugeiconsIcon icon={Download01Icon} size={13} />
                {t("file.upload.downloadSelected")}
              </a>
            </Button>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="h-7 gap-1.5 text-[11px] text-destructive hover:text-destructive"
              disabled={busy || deletable.length === 0}
              onClick={() => onDeleteSelected(deletable)}
            >
              <HugeiconsIcon icon={Delete01Icon} size={13} />
              {/* `_`, since the key has a `confirm` subkey and so becomes a namespace. */}
              {t("file.upload.deleteSelected._")}
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
