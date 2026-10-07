"use client";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileImportIcon } from "@hugeicons/core-free-icons";
import { leafKeyOf } from "@/lib/leaf-key";
import {
  downloadEmployeeSalaryCostAssignmentsExcel,
  downloadEmployeeSalaryExcel,
} from "@/lib/rs/employee-salary";
import type { MagicFilter } from "@/lib/rs/types";
import { useUpdateAccess } from "@/hooks/use-update-access";
import { Button } from "@/components/ui/button";
import { ExcelExportButton } from "@/components/shared/excel-export-button";
import { navigateInGesture } from "@/lib/navigate-in-gesture";

/**
 * The two exports Wicket's salary list offers in its content menu: one row per salary, and the
 * cost-assignment (DATEV) sheet. Both act on the filter the list is showing, which is why they live in its
 * toolbar and are handed that filter (see PageDef.listActions).
 */
export function EmployeeSalaryListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const router = useRouter();
  // The salary import writes salaries, so its button appears only for a user who may change them
  // (HR_EMPLOYEE_SALARY write, reported as listMeta.userAccess.update). The endpoint enforces it too.
  const canImport = useUpdateAccess("employeeSalary");

  return (
    <>
      {canImport && (
        <Button
          type="button"
          variant="outline"
          onClick={() => navigateInGesture(router, "/employeeSalary/import")}
        >
          <HugeiconsIcon icon={FileImportIcon} />
          {t("import._")}
        </Button>
      )}
      <ExcelExportButton download={() => downloadEmployeeSalaryExcel(filter)} />
      <ExcelExportButton
        download={() => downloadEmployeeSalaryCostAssignmentsExcel(filter)}
        tooltip={t("fibu.employee.salary.exportXls.tooltip")}
        // The label is the parent of that tooltip key, so it travels as the generator's leaf.
        label={t(leafKeyOf("fibu.rechnung.kostExcelExport", t.has))}
      />
    </>
  );
}
