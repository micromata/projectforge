"use client";

import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import { FormAlert } from "@/components/shared/form-alert";
import {
  dataTransferViewQueryKey,
  type DataTransferView,
} from "@/lib/rs/datatransfer";

/**
 * The files of an area: upload, download, delete — the tab every user with access works in.
 *
 * An area with external access via the gateway has its files there: a note links to it, and there's no
 * upload here (files stored before stay downloadable and deletable). On the gateway, an area pushed by the
 * main instance is administered there, which a note says.
 */
export function DataTransferFilesTab({ view }: { view: DataTransferView }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const id = view.area.id!;

  return (
    <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">
      {view.gatewayUrl && (
        <FormAlert tone="info">
          {t("plugins.datatransfer.gateway.files.onGateway")}{" "}
          <a
            href={view.gatewayUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="underline"
          >
            {view.gatewayUrl}
          </a>
        </FormAlert>
      )}
      {view.managedByMainInstance && (
        <FormAlert tone="info">
          {t("plugins.datatransfer.gateway.managedByMainInstance")}
        </FormAlert>
      )}
      <AttachmentList
        noUpload={!!view.gatewayUrl}
        entity="datatransfer"
        id={id}
        // An area may hold many files: sortable and searchable like the legacy grid.
        layout="table"
        // One column layout for all areas (DataTransferFilesRest.columnStates).
        columnStates="datatransferfiles"
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
