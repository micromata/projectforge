"use client";

import { useRef, useState, type DragEvent, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CloudUploadIcon } from "@hugeicons/core-free-icons";

interface Props {
  /** Called once with all dropped files — the endpoint takes a single file per call. */
  onFiles: (files: File[]) => void;
  /** No drop target at all — for a read-only list. */
  disabled?: boolean;
  children: ReactNode;
}

/**
 * Makes a whole attachment section a drop target, without costing it any height: the dashed area only
 * appears — as an overlay — while files are being dragged over it.
 *
 * Replaces the permanent drop box of the first version. Inline between the fields of a form the box
 * was the tallest thing in the section while the files themselves are what one comes for; picking
 * files by click is the compact button next to the other actions now (see AttachmentAddButton).
 *
 * Keyboard operable it is not, and does not need to be: a drop is a mouse gesture, and the button is
 * the path everything else takes.
 *
 * Size is not checked here. The limit is the backend's (`FileSizeChecker`, configurable per
 * installation via `projectforge.jcr.maxDefaultFileSize`), it rejects an oversized file with a
 * translated message, and duplicating the number here would be a second place to get it wrong.
 */
export function AttachmentDropZone({ onFiles, disabled, children }: Props) {
  const t = useTranslations();
  /**
   * Enter and leave are counted instead of a plain boolean: dragging from the section into one of its
   * rows fires leave on the section, which would switch the overlay off in the middle of the drag.
   */
  const depth = useRef(0);
  const [over, setOver] = useState(false);

  /** Text or a link being dragged is none of our business — only a file drop shows the overlay. */
  function draggingFiles(e: DragEvent): boolean {
    return e.dataTransfer.types.includes("Files");
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    depth.current = 0;
    setOver(false);
    const files = Array.from(e.dataTransfer.files);
    if (files.length > 0) onFiles(files);
  }

  if (disabled) return <>{children}</>;

  return (
    <div
      className="relative"
      onDragEnter={(e) => {
        if (!draggingFiles(e)) return;
        depth.current += 1;
        setOver(true);
      }}
      onDragOver={(e) => {
        // Without preventDefault the browser navigates to the dropped file instead.
        if (draggingFiles(e)) e.preventDefault();
      }}
      onDragLeave={() => {
        depth.current -= 1;
        if (depth.current <= 0) {
          depth.current = 0;
          setOver(false);
        }
      }}
      onDrop={onDrop}
    >
      {children}
      {over && (
        // pointer-events-none, so the drop itself still reaches this container: an overlay swallowing
        // it would end the drag with nothing happening.
        <div className="pointer-events-none absolute -inset-1 z-20 flex flex-col items-center justify-center gap-1.5 rounded-md border border-dashed border-primary bg-primary/10 text-xs font-medium text-foreground">
          <HugeiconsIcon icon={CloudUploadIcon} size={20} />
          {t("attachment.upload.dropHere")}
        </div>
      )}
    </div>
  );
}
