"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { toast } from "@/lib/toast";
import { leafKeyOf } from "@/lib/leaf-key";
import { RsError } from "@/lib/rs/client";
import {
  downloadEmployeeSalaryCostAssignmentsExcel,
  downloadEmployeeSalaryExcel,
} from "@/lib/rs/employee-salary";
import type { MagicFilter } from "@/lib/rs/types";
import { ExportButton } from "@/components/shared/export-button";

/**
 * The two exports Wicket's salary list offers in its content menu: one row per salary, and the
 * cost-assignment (DATEV) sheet. Both act on the filter the list is showing, which is why they live in its
 * toolbar and are handed that filter (see PageDef.listActions).
 */
export function EmployeeSalaryListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();

  /**
   * A 404 is no error here: the filter matched nothing, so there is nothing to export. Everything else -
   * including the 400 the cost-assignment export answers with when no month is picked - carries its own
   * message and is shown as it is.
   */
  const onError = (error: unknown) => {
    if (error instanceof RsError && error.status === 404) {
      toast.info(t("datatable.no-records-found"));
      return;
    }
    toast.error(error instanceof Error ? error.message : String(error));
  };

  const excel = useMutation({
    mutationFn: () => downloadEmployeeSalaryExcel(filter),
    onError,
  });
  const costAssignments = useMutation({
    mutationFn: () => downloadEmployeeSalaryCostAssignmentsExcel(filter),
    onError,
  });

  return (
    <>
      <ExportButton
        tooltip={t("tooltip.export.excel")}
        label={t("exportAsXls")}
        isPending={excel.isPending}
        onClick={() => excel.mutate()}
      />
      <ExportButton
        tooltip={t("fibu.employee.salary.exportXls.tooltip")}
        // The label is the parent of that tooltip key, so it travels as the generator's leaf.
        label={t(leafKeyOf("fibu.rechnung.kostExcelExport", t.has))}
        isPending={costAssignments.isPending}
        onClick={() => costAssignments.mutate()}
      />
    </>
  );
}
