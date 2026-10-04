"use client";

import { useTranslations } from "next-intl";
import { useExportDownload } from "@/hooks/use-export-download";
import { ExportButton } from "@/components/shared/export-button";

/**
 * The Excel export of a list toolbar (see PageDef.listActions): a download button that acts on the filter
 * the list is showing, with the error handling of [useExportDownload].
 *
 * `tooltip` and `label` default to the plain list export's; an export of another shape (the cost
 * assignments of the invoice lists) passes its own.
 */
export function ExcelExportButton({
  download,
  tooltip,
  label,
}: {
  download: () => Promise<void>;
  tooltip?: string;
  label?: string;
}) {
  const t = useTranslations();
  const excel = useExportDownload(download);
  return (
    <ExportButton
      tooltip={tooltip ?? t("tooltip.export.excel")}
      label={label ?? t("exportAsXls")}
      isPending={excel.isPending}
      onClick={() => excel.mutate()}
    />
  );
}
