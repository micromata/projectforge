"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";

export default function TimesheetListPage() {
  return <EntityListPage page={TIMESHEET_PAGE} />;
}
