"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowLeft01Icon,
  ArrowRight01Icon,
  Download01Icon,
  LinkSquare02Icon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  attachmentDownloadUrl,
  attachmentPreviewUrl,
  type Attachment,
  type AttachmentRef,
} from "@/lib/rs/attachments";
import { AttachmentPreview } from "./attachment-preview";

/**
 * The files of a list shown in the browser — PDFs in the viewer of the browser, images, texts — with download,
 * "open in new tab", and paging through all files of the list that can be shown (buttons, arrow keys).
 *
 * The backend decides by the content what is shown (`AttachmentPreview`) and answers 415 for anything else;
 * [previewKind] only picks the files the viewer is offered for.
 */
export function AttachmentViewer({
  files,
  index,
  entity,
  id,
  base,
  onIndexChange,
  onClose,
}: {
  /** The files to page through, all of them with a [previewKind]. */
  files: Attachment[];
  index: number | null;
  entity: string;
  id: number;
  base?: string;
  onIndexChange: (index: number) => void;
  onClose: () => void;
}) {
  const t = useTranslations();
  const file = index != null ? (files[index] ?? null) : null;
  const count = files.length;
  // On the window, not the dialog: paging to the first or last file disables the focused pager button, which
  // drops the focus out of the dialog (as in the image lightbox of the chat).
  useEffect(() => {
    if (index == null || count < 2) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "ArrowLeft" && index > 0) onIndexChange(index - 1);
      if (event.key === "ArrowRight" && index < count - 1)
        onIndexChange(index + 1);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [index, count, onIndexChange]);
  const ref: AttachmentRef | null = file
    ? { entity, id, fileId: file.fileId, base }
    : null;
  return (
    <Dialog open={file != null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="flex h-[95dvh] max-w-[95vw] flex-col gap-2 p-3 sm:max-w-[95vw]">
        <DialogTitle className="truncate pr-8 text-sm">
          {file?.name}
        </DialogTitle>
        <DialogDescription className="sr-only">
          {t("attachment.view._")}
        </DialogDescription>
        {file && ref && (
          <>
            <div className="min-h-0 flex-1 overflow-auto rounded-md border bg-muted/30">
              {/* Keyed by the file: a text or a failed image must not stay when paging on. */}
              <AttachmentPreview key={file.fileId} file={file} fileRef={ref} />
            </div>
            <div className="flex flex-wrap items-center justify-end gap-2">
              {count > 1 && (
                <div className="mr-auto flex items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="icon-sm"
                    aria-label={t("attachment.view.previous")}
                    disabled={index! <= 0}
                    onClick={() => onIndexChange(index! - 1)}
                  >
                    <HugeiconsIcon icon={ArrowLeft01Icon} />
                  </Button>
                  <span className="text-xs text-muted-foreground tabular-nums">
                    {index! + 1} / {count}
                  </span>
                  <Button
                    type="button"
                    variant="outline"
                    size="icon-sm"
                    aria-label={t("attachment.view.next")}
                    disabled={index! >= count - 1}
                    onClick={() => onIndexChange(index! + 1)}
                  >
                    <HugeiconsIcon icon={ArrowRight01Icon} />
                  </Button>
                </div>
              )}
              <NewTabButton fileRef={ref} />
              <DownloadButton fileRef={ref} />
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

function NewTabButton({ fileRef }: { fileRef: AttachmentRef }) {
  const t = useTranslations();
  return (
    <Button asChild variant="outline" size="sm">
      <a
        href={attachmentPreviewUrl(fileRef)}
        target="_blank"
        rel="noopener noreferrer"
      >
        <HugeiconsIcon icon={LinkSquare02Icon} data-icon="inline-start" />
        {t("attachment.view.newTab")}
      </a>
    </Button>
  );
}

function DownloadButton({ fileRef }: { fileRef: AttachmentRef }) {
  const t = useTranslations();
  return (
    <Button asChild variant="outline" size="sm">
      {/* A link, not a fetch: the answer is the file, which the browser has to save itself. */}
      <a href={attachmentDownloadUrl(fileRef)}>
        <HugeiconsIcon icon={Download01Icon} data-icon="inline-start" />
        {t("download._")}
      </a>
    </Button>
  );
}
