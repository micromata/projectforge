"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { HR_PLANNING_LIST_PAGE } from "@/components/features/hr-planning/hr-planning-list.page";

export default function HRPlanningListPage() {
  return <EntityListPage page={HR_PLANNING_LIST_PAGE} />;
}
