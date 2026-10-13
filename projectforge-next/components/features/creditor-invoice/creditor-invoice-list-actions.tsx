"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileImportIcon } from "@hugeicons/core-free-icons";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  downloadCreditorInvoiceCostAssignmentsExcel,
  downloadCreditorInvoiceExcel,
} from "@/lib/rs/creditor-invoice";
import type { MagicFilter } from "@/lib/rs/types";
import { Button } from "@/components/ui/button";
import {
  ExcelExportMenuItem,
  ExportMenu,
  ExportMenuItem,
} from "@/components/shared/export-menu";
import { useExportDownload } from "@/hooks/use-export-download";
import { navigateInGesture } from "@/lib/navigate-in-gesture";

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
  const excel = useExportDownload(() => downloadCreditorInvoiceExcel(filter));
  const costAssignments = useExportDownload(() =>
    downloadCreditorInvoiceCostAssignmentsExcel(filter)
  );

  return (
    <>
      <Button
        type="button"
        variant="outline"
        onClick={() => navigateInGesture(router, "/creditor-invoice-import")}
      >
        <HugeiconsIcon icon={FileImportIcon} />
        {t("import._")}
      </Button>
      <ExportMenu isPending={excel.isPending || costAssignments.isPending}>
        <ExcelExportMenuItem onSelect={() => excel.mutate()} />
        <ExportMenuItem
          // The label is the parent of the tooltip key, so it travels as the generator's leaf.
          label={t(leafKeyOf("fibu.rechnung.kostExcelExport", t.has))}
          description={t("fibu.rechnung.kostExcelExport.tooltip")}
          onSelect={() => costAssignments.mutate()}
        />
      </ExportMenu>
    </>
  );
}
