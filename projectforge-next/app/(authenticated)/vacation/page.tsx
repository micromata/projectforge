"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { VACATION_PAGE } from "@/components/features/vacation/vacation.page";

export default function VacationListPage() {
  return <EntityListPage page={VACATION_PAGE} />;
}
