"use client";

import { useMemo } from "react";
import { ImportFeature } from "@/components/shared/import/import-feature";
import type { ImportConfig } from "@/components/shared/import/import-types";
import { ENTITY } from "@/lib/rs/employee-salary-import";
import { EMPLOYEE_SALARY_IMPORT_COLUMNS } from "./columns";

/**
 * The employee-salary (Gehaltsimport) xlsx import. A thin consumer of the generic {@link ImportFeature}: it
 * only supplies the [ImportConfig] — the REST base (`employeeSalaryImport`), the column layout and where a
 * commit returns to (the salary list). Every screen, mutation and job handoff lives in the shared module.
 */
export function EmployeeSalaryImport() {
  const config = useMemo<ImportConfig>(
    () => ({
      endpoints: { base: ENTITY },
      titleKey: "fibu.employee.salaries.import",
      columns: EMPLOYEE_SALARY_IMPORT_COLUMNS,
      fileAccept: ".xlsx,.xls",
      returnRoute: "/employeeSalary",
    }),
    []
  );

  return <ImportFeature config={config} />;
}
