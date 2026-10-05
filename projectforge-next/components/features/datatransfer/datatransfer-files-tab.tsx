"use client";

import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import {
  dataTransferViewQueryKey,
  type DataTransferView,
} from "@/lib/rs/datatransfer";

/** The files of an area: upload, download, delete — the tab every user with access works in. */
export function DataTransferFilesTab({ view }: { view: DataTransferView }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const id = view.area.id!;

  return (
    <div className="min-h-0 flex-1 overflow-y-auto p-4">
      <AttachmentList
        entity="datatransfer"
        id={id}
        // An area may hold many files: sortable and searchable like the legacy grid.
        layout="table"
        uploadHint={`${t("plugins.datatransfer.maxUploadSize._")}: ${
          view.area.capacity?.maxUploadSizeFormatted ?? ""
        }`}
        // Size, capacity and the "download all" depend on the files.
        onChanged={() =>
          void queryClient.invalidateQueries({
            queryKey: dataTransferViewQueryKey(id),
          })
        }
      />
    </div>
  );
}
