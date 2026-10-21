"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { AttachmentDropArea } from "@/components/shared/attachments/attachment-drop-area";
import { FieldHint } from "@/components/shared/form/field-hint";
import { Spinner } from "@/components/shared/spinner";
import { executeMerlinSerial } from "@/lib/rs/merlin";
import { toast } from "@/lib/toast";

/**
 * The serial execution: an Excel® file (see the downloads of the page's heading) with one row per document
 * is dropped here, and the documents of all rows come back as a ZIP archive. Rows with invalid values are
 * skipped and named in the Merlin log.
 */
export function MerlinSerialExecution({ id }: { id: number }) {
  const t = useTranslations();
  const label = t("plugins.merlin.upload.serialExecution._");
  const serial = useMutation({
    mutationFn: (file: File) => executeMerlinSerial(id, file),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-1 text-sm font-medium">
        {label}
        <FieldHint
          hint={t("plugins.merlin.upload.serialExecution.info")}
          label={label}
        />
        {serial.isPending && <Spinner className="ml-2 h-4 w-4 border-2" />}
      </div>
      <AttachmentDropArea
        onFiles={(files) => files[0] && serial.mutate(files[0])}
        accept=".xlsx,.xls"
        multiple={false}
        disabled={serial.isPending}
        label={label}
      />
    </div>
  );
}
