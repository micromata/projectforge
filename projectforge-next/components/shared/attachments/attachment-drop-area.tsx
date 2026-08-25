"use client";

import { useRef, useState, type DragEvent } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CloudUploadIcon } from "@hugeicons/core-free-icons";
import { cn } from "@/lib/utils";

interface Props {
  /** Called once per dropped/chosen file — the endpoint takes a single file per call. */
  onFiles: (files: File[]) => void;
  disabled?: boolean;
  /** `<input accept>` filter, e.g. `.csv`. Omitted → any file (the default). */
  accept?: string;
  /** Whether more than one file may be chosen at once. Defaults to true. */
  multiple?: boolean;
  /** Overrides the label inside the area (defaults to the attachment upload text). */
  label?: string;
}

/**
 * Click-or-drop area for uploading attachments.
 *
 * A `<button>` wrapping a hidden `<input type="file">` rather than a styled label: the drop target
 * has to be operable by keyboard too, and a label reachable by Tab is not something screen readers
 * announce as an action.
 *
 * Size is not checked here. The limit is the backend's (`FileSizeChecker`, configurable per
 * installation via `projectforge.jcr.maxDefaultFileSize`), it rejects an oversized file with a
 * translated message, and duplicating the number here would be a second place to get it wrong.
 */
export function AttachmentDropArea({
  onFiles,
  disabled,
  accept,
  multiple = true,
  label,
}: Props) {
  const t = useTranslations();
  const inputRef = useRef<HTMLInputElement>(null);
  const [over, setOver] = useState(false);

  function acceptFiles(files: FileList | null) {
    const list = Array.from(files ?? []);
    if (list.length > 0) onFiles(list);
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    setOver(false);
    if (!disabled) acceptFiles(e.dataTransfer.files);
  }

  return (
    <>
      <button
        type="button"
        disabled={disabled}
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => {
          // Without preventDefault the browser navigates to the dropped file instead.
          e.preventDefault();
          if (!disabled) setOver(true);
        }}
        onDragLeave={() => setOver(false)}
        onDrop={onDrop}
        className={cn(
          "flex w-full flex-col items-center gap-1.5 rounded-md border border-dashed border-input bg-background/60 px-4 py-6",
          "text-xs text-muted-foreground transition-colors",
          "hover:border-primary/50 hover:text-foreground",
          "focus-visible:border-ring focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30",
          over && "border-primary bg-primary/5 text-foreground",
          disabled && "pointer-events-none opacity-50"
        )}
      >
        <HugeiconsIcon icon={CloudUploadIcon} size={20} />
        {label ?? t("attachment.upload.title")}
      </button>
      <input
        ref={inputRef}
        type="file"
        multiple={multiple}
        accept={accept}
        // sr-only rather than hidden: a hidden input is not focusable, and the file dialog of some
        // browsers refuses to open from one.
        className="sr-only"
        aria-label={t("file.upload.choose")}
        onChange={(e) => {
          acceptFiles(e.target.files);
          // Clears the selection so choosing the same file twice fires change again — the second
          // attempt is what surfaces the backend's duplicate-name message.
          e.target.value = "";
        }}
      />
    </>
  );
}
