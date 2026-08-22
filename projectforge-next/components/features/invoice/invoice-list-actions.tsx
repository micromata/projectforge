"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { leafKeyOf } from "@/lib/leaf-key";
import { RsError } from "@/lib/rs/client";
import {
  downloadInvoiceCostAssignmentsExcel,
  downloadInvoiceExcel,
} from "@/lib/rs/invoice";
import type { MagicFilter } from "@/lib/rs/types";
import { InvoiceExportButton } from "./invoice-export-button";

/**
 * The two exports of the invoice list, as Wicket's list page offers them in its content menu: one row per
 * invoice, and one row per cost assignment.
 *
 * Both act on the filter the list is showing, which is why they live in its toolbar and are handed that
 * filter (see PageDef.listActions).
 */
export function InvoiceListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();

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
    mutationFn: () => downloadInvoiceExcel(filter),
    onError,
  });
  const costAssignments = useMutation({
    mutationFn: () => downloadInvoiceCostAssignmentsExcel(filter),
    onError,
  });

  return (
    <>
      <InvoiceExportButton
        tooltip={t("tooltip.export.excel")}
        label={t("exportAsXls")}
        isPending={excel.isPending}
        onClick={() => excel.mutate()}
      />
      <InvoiceExportButton
        tooltip={t("fibu.rechnung.kostExcelExport.tooltip")}
        // The label is the parent of that tooltip key, so it travels as the generator's leaf.
        label={t(leafKeyOf("fibu.rechnung.kostExcelExport", t.has))}
        isPending={costAssignments.isPending}
        onClick={() => costAssignments.mutate()}
      />
    </>
  );
}
