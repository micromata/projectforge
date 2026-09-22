"use client";

import { useTranslations } from "next-intl";
import { Progress } from "@/components/ui/progress";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";
import type { ImportConfig } from "./import-types";

interface Props {
  config: ImportConfig;
  onFile: (file: File) => void;
  /** 0–100 while a file is uploading, null when idle. */
  uploadProgress: number | null;
}

/**
 * The first screen: drop or choose the file to import. Reuses [AttachmentDropArea] (restricted to the
 * config's single accepted type) so this looks and behaves like every other upload in the app, and shows
 * the XHR progress while the file travels — the parse itself happens server-side and lands as the view.
 */
export function ImportDropStep({ config, onFile, uploadProgress }: Props) {
  const t = useTranslations();
  const uploading = uploadProgress !== null;

  return (
    <div className="flex flex-col gap-3">
      <AttachmentDropArea
        onFiles={(files) => files[0] && onFile(files[0])}
        accept={config.fileAccept}
        multiple={false}
        disabled={uploading}
        label={t(config.titleKey)}
      />
      {uploading && (
        <div className="flex items-center gap-2">
          <Progress value={uploadProgress ?? 0} className="flex-1" />
          <span className="w-12 text-right text-xs tabular-nums text-muted-foreground">
            {uploadProgress ?? 0} %
          </span>
        </div>
      )}
    </div>
  );
}
