"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { EMPLOYEE_PAGE } from "@/components/features/employee/employee.page";

export default function EmployeeListPage() {
  return <EntityListPage page={EMPLOYEE_PAGE} />;
}
