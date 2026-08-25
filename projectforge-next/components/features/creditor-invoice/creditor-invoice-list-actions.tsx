"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileImportIcon } from "@hugeicons/core-free-icons";
import { toast } from "@/lib/toast";
import { leafKeyOf } from "@/lib/leaf-key";
import { RsError } from "@/lib/rs/client";
import {
  downloadCreditorInvoiceCostAssignmentsExcel,
  downloadCreditorInvoiceExcel,
} from "@/lib/rs/creditor-invoice";
import type { MagicFilter } from "@/lib/rs/types";
import { Button } from "@/components/ui/button";
import { ExportButton } from "@/components/shared/export-button";

/**
 * The two exports of the incoming invoice list, as Wicket's list page offers them in its content menu: one
 * row per invoice, and one row per cost assignment.
 *
 * Both act on the filter the list is showing, which is why they live in its toolbar and are handed that
 * filter (see PageDef.listActions).
 */
export function CreditorInvoiceListActions({
  filter,
}: {
  filter: MagicFilter;
}) {
  const t = useTranslations();
  const router = useRouter();

  /**
   * A 404 is no error here: the filter matched nothing, or — for the cost assignments — the installation
   * has no cost ids configured, in which case that export has nothing to say.
   */
  const onError = (error: unknown) => {
    if (error instanceof RsError && error.status === 404) {
      toast.info(t("datatable.no-records-found"));
      return;
    }
    toast.error(error instanceof Error ? error.message : String(error));
  };

  const excel = useMutation({
    mutationFn: () => downloadCreditorInvoiceExcel(filter),
    onError,
  });
  const costAssignments = useMutation({
    mutationFn: () => downloadCreditorInvoiceCostAssignmentsExcel(filter),
    onError,
  });

  return (
    <>
      <Button
        type="button"
        variant="outline"
        onClick={() => router.push("/creditor-invoice-import")}
      >
        <HugeiconsIcon icon={FileImportIcon} />
        {t("import._")}
      </Button>
      <ExportButton
        tooltip={t("tooltip.export.excel")}
        label={t("exportAsXls")}
        isPending={excel.isPending}
        onClick={() => excel.mutate()}
      />
      <ExportButton
        tooltip={t("fibu.rechnung.kostExcelExport.tooltip")}
        // The label is the parent of that tooltip key, so it travels as the generator's leaf.
        label={t(leafKeyOf("fibu.rechnung.kostExcelExport", t.has))}
        isPending={costAssignments.isPending}
        onClick={() => costAssignments.mutate()}
      />
    </>
  );
}
