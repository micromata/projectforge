"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { ExportButton } from "@/components/shared/export-button";
import { LogViewerLink } from "@/components/shared/log-viewer-link";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useListMeta } from "@/hooks/use-list-meta";
import {
  exportMerlinExcelTemplate,
  type MerlinTemplate,
} from "@/lib/rs/merlin";
import { toast } from "@/lib/toast";

/**
 * Beside the heading of the form: the Excel® template definition of the variables as they are in the form
 * (to configure the template in Excel® and upload it again), and the Merlin log of the user.
 */
export function MerlinEditHeaderActions() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const logViewerUrl = useListMeta("merlin").data?.variables?.logViewerUrl as
    | string
    | undefined;
  const excel = useMutation({
    mutationFn: () =>
      exportMerlinExcelTemplate(form.state.values as MerlinTemplate),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  return (
    <>
      <LogViewerLink url={logViewerUrl} label={t("plugins.merlin.viewLogs")} />
      <ExportButton
        label={t("plugins.merlin.exportExcelTemplate._")}
        tooltip={t("plugins.merlin.exportExcelTemplate.info")}
        isPending={excel.isPending}
        onClick={() => excel.mutate()}
      />
    </>
  );
}
