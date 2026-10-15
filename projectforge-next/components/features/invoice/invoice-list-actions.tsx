"use client";

import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  downloadInvoiceCostAssignmentsExcel,
  downloadInvoiceExcel,
} from "@/lib/rs/invoice";
import type { MagicFilter } from "@/lib/rs/types";
import { useUpdateAccess } from "@/hooks/use-update-access";
import {
  ExcelExportMenuItem,
  ExportMenu,
  ExportMenuItem,
} from "@/components/shared/export-menu";
import { useExportDownload } from "@/hooks/use-export-download";
import { EInvoiceCheckerButton } from "./e-invoice-checker-button";

/**
 * The two exports of the invoice list, as Wicket's list page offers them in its content menu: one row per
 * invoice, and one row per cost assignment — and the e-invoice checker beside them.
 *
 * Both exports act on the filter the list is showing, which is why they live in its toolbar and are handed
 * that filter (see PageDef.listActions). The checker takes no filter and no invoice at all: it is here
 * because this is the page one exports e-invoices from, and reading one back is the same errand.
 */
export function InvoiceListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  // The cost-assignment export is finance data: hidden from a read-only viewer (an order-book user,
  // whose rest class reports `update: false`), kept for finance/controlling. The plain Excel export
  // stays — exporting the rows one may already see is no more than the list itself shows.
  const updateAccess = useUpdateAccess("outgoingInvoice");
  const excel = useExportDownload(() => downloadInvoiceExcel(filter));
  const costAssignments = useExportDownload(() =>
    downloadInvoiceCostAssignmentsExcel(filter)
  );

  return (
    <>
      <EInvoiceCheckerButton />
      <ExportMenu isPending={excel.isPending || costAssignments.isPending}>
        <ExcelExportMenuItem onSelect={() => excel.mutate()} />
        {updateAccess !== false && (
          <ExportMenuItem
            // The label is the parent of the tooltip key, so it travels as the generator's leaf.
            label={t(leafKeyOf("fibu.rechnung.kostExcelExport", t.has))}
            description={t("fibu.rechnung.kostExcelExport.tooltip")}
            onSelect={() => costAssignments.mutate()}
          />
        )}
      </ExportMenu>
    </>
  );
}
