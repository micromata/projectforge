"use client";

import { useRef } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CloudUploadIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";

interface Props {
  /** Called once with all chosen files — the endpoint takes a single file per call. */
  onFiles: (files: File[]) => void;
  disabled?: boolean;
}

/**
 * Picks files to attach — the click half of the upload, sitting in the section's action bar next to
 * the download and delete actions (see AttachmentToolbar). Dropping them is the whole section's job
 * (see AttachmentDropZone).
 *
 * A `<button>` driving a hidden `<input type="file">` rather than a styled label: a label reachable by
 * Tab is not something screen readers announce as an action.
 *
 * The tooltip is where dropping is mentioned at all: the old permanent box said so by standing there,
 * and once it is gone nothing else would ever tell the user that the section takes a drop.
 */
export function AttachmentAddButton({ onFiles, disabled }: Props) {
  const t = useTranslations();
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <>
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-7 gap-1.5 text-[11px]"
            disabled={disabled}
            onClick={() => inputRef.current?.click()}
          >
            <HugeiconsIcon icon={CloudUploadIcon} size={13} />
            {/* `_`, since the key has an `info` subkey and so becomes a namespace. */}
            {t("attachment.upload.add._")}
          </Button>
        </TooltipTrigger>
        <TooltipContent>{t("attachment.upload.add.info")}</TooltipContent>
      </Tooltip>
      <input
        ref={inputRef}
        type="file"
        multiple
        // sr-only rather than hidden: a hidden input is not focusable, and the file dialog of some
        // browsers refuses to open from one.
        className="sr-only"
        aria-label={t("file.upload.choose")}
        onChange={(e) => {
          const files = Array.from(e.target.files ?? []);
          if (files.length > 0) onFiles(files);
          // Clears the selection so choosing the same file twice fires change again — the second
          // attempt is what surfaces the backend's duplicate-name message.
          e.target.value = "";
        }}
      />
    </>
  );
}
