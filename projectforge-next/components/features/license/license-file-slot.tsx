"use client";

import { useRef } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CloudUploadIcon, Delete02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";
import { AttachmentDropZone } from "@/components/shared/attachments/attachment-drop-zone";
import { toast } from "@/lib/toast";

/**
 * One of the two file slots of a license: the stored name (a download), a button to store another file
 * in its place and one to remove it. Takes a file through the button and by a drop onto the row, the way
 * the invoice PDF field does.
 */
export function LicenseFileSlot({
  label,
  filename,
  busy,
  uploading,
  removing,
  onDownload,
  onUpload,
  onRemove,
}: {
  label: string;
  filename: string | null | undefined;
  /** Any write of the two slots is running — one at a time, they share the license row. */
  busy: boolean;
  uploading: boolean;
  removing: boolean;
  onDownload: () => void;
  onUpload: (file: File) => void;
  onRemove: () => void;
}) {
  const t = useTranslations();
  const inputRef = useRef<HTMLInputElement>(null);

  // A slot holds exactly one file, so a drop of several is refused rather than guessed at.
  const onDropped = (files: File[]) => {
    if (files.length > 1) {
      toast.error(t("file.upload.error.tooManyFiles"));
      return;
    }
    onUpload(files[0]);
  };

  return (
    <AttachmentDropZone onFiles={onDropped} disabled={busy}>
      <div className="flex flex-wrap items-center gap-2">
        <span className="min-w-16 text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        {filename ? (
          <button
            type="button"
            className="text-sm hover:underline"
            aria-label={`${t("download._")}: ${label}`}
            onClick={onDownload}
          >
            {filename}
          </button>
        ) : (
          <span className="text-sm text-muted-foreground">
            {t("nothingFound")}
          </span>
        )}
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="h-7 gap-1.5 text-[11px]"
          disabled={busy}
          aria-label={`${t("file.upload.choose")}: ${label}`}
          onClick={() => inputRef.current?.click()}
        >
          {uploading ? (
            <Spinner className="h-3 w-3 border-2" />
          ) : (
            <HugeiconsIcon icon={CloudUploadIcon} size={13} />
          )}
          {t("file.upload.choose")}
        </Button>
        {filename && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-7 gap-1.5 text-[11px] text-destructive"
            disabled={busy}
            aria-label={`${t("delete")}: ${label}`}
            onClick={onRemove}
          >
            {removing ? (
              <Spinner className="h-3 w-3 border-2" />
            ) : (
              <HugeiconsIcon icon={Delete02Icon} size={13} />
            )}
            {t("delete")}
          </Button>
        )}
        <input
          ref={inputRef}
          type="file"
          // Out of the tab order and the accessibility tree: the button above is the control that opens it,
          // and a second one of the same name would only be announced twice.
          className="sr-only"
          tabIndex={-1}
          aria-hidden
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) onUpload(file);
            // Cleared so choosing the same file twice fires change again.
            e.target.value = "";
          }}
        />
      </div>
    </AttachmentDropZone>
  );
}
