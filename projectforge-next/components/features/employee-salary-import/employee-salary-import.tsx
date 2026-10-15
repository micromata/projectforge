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
      // Both a leaf value ("Gehälterimport") and a branch (…import.format.*), so the generator nests the
      // value under the reserved "_" — read it as such (see chart._, login._).
      titleKey: "fibu.employee.salaries.import._",
      columns: EMPLOYEE_SALARY_IMPORT_COLUMNS,
      fileAccept: ".xlsx,.xls",
      returnRoute: "/employeeSalary",
    }),
    []
  );

  return <ImportFeature config={config} />;
}
