"use client";

import { useTranslations } from "next-intl";
import { PageShell } from "@/components/shared/page-shell";
import { EmployeeSalaryImport } from "@/components/features/employee-salary-import/employee-salary-import";
import { EmployeeSalaryImportFormatHint } from "@/components/features/employee-salary-import/format-hint";

/**
 * The employee-salary (Gehaltsimport) xlsx import (`/next/employeeSalary/import`), reached from the salary
 * list's action bar rather than a menu entry. A concrete route rather than a list category: the import is
 * no REST list, and the flow lives in the shared import module (see components/shared/import).
 *
 * HR only. Enforced by the endpoints behind it (`EmployeeSalaryImportRest`, HR_EMPLOYEE_SALARY write); this
 * page and the button that leads here merely don't offer what would answer 403.
 */
export default function EmployeeSalaryImportPage() {
  const t = useTranslations();

  return (
    <PageShell>
      <div className="flex items-center gap-3 border-b bg-background px-4 py-3">
        <h1 className="text-lg font-bold tracking-tight">
          {t("fibu.employee.salaries.import._")}
        </h1>
        <div className="flex-1" />
      </div>
      <div className="flex min-h-0 flex-1 flex-col gap-3 p-4">
        <EmployeeSalaryImport />
        <EmployeeSalaryImportFormatHint />
      </div>
    </PageShell>
  );
}
