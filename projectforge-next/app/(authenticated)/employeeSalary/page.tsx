"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { EMPLOYEE_SALARY_PAGE } from "@/components/features/employee-salary/employee-salary.page";

export default function EmployeeSalaryListPage() {
  return <EntityListPage page={EMPLOYEE_SALARY_PAGE} />;
}
