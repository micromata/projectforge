"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete01Icon, Download01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { attachmentsDownloadUrl, type Attachment } from "@/lib/rs/attachments";
import type { AttachmentSelection } from "@/hooks/use-attachment-selection";

interface Props {
  attachments: Attachment[];
  selection: AttachmentSelection;
  entity: string;
  id: number;
  /** Asks for the selected files to be deleted — the confirmation is the list's. */
  onDeleteSelected: (attachments: Attachment[]) => void;
  busy?: boolean;
}

/**
 * Select-all plus the actions that work on a whole selection: download as one ZIP, delete at once.
 *
 * The two buttons stay visible and merely disabled while nothing is picked, so it is discoverable
 * that a selection has actions at all — a bar that only appears once something is selected leaves
 * the checkboxes looking purposeless.
 *
 * Deleting a file the user may not touch is not offered: `Attachment.readonly` marks those, and the
 * backend would silently skip them (`multiDelete` filters by access), leaving a partial result that
 * looks like a failure.
 */
export function AttachmentSelectionBar({
  attachments,
  selection,
  entity,
  id,
  onDeleteSelected,
  busy,
}: Props) {
  const t = useTranslations();
  const { selected } = selection;
  const allSelected =
    attachments.length > 0 && selected.length === attachments.length;
  const deletable = selected.filter((a) => a.readonly !== true);
  const fileIds = selected.map((a) => a.fileId);

  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-2 border-b border-border/60 pb-2">
      <div className="flex items-center gap-2">
        <Checkbox
          id={`${entity}-${id}-select-all`}
          checked={
            allSelected ? true : selected.length > 0 ? "indeterminate" : false
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
        {/* A link, not a fetch: the answer is the ZIP itself (see attachmentsDownloadUrl). Disabled
            state is an anchor without href — `disabled` means nothing on one. */}
        <Button
          asChild={fileIds.length > 0}
          variant="outline"
          size="sm"
          className="h-7 gap-1.5 text-[11px]"
          disabled={fileIds.length === 0}
        >
          {fileIds.length > 0 ? (
            <a href={attachmentsDownloadUrl(entity, id, fileIds)}>
              <HugeiconsIcon icon={Download01Icon} size={13} />
              {t("file.upload.downloadSelected")}
            </a>
          ) : (
            <span>
              <HugeiconsIcon icon={Download01Icon} size={13} />
              {t("file.upload.downloadSelected")}
            </span>
          )}
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
    </div>
  );
}
