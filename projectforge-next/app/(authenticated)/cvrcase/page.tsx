"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { CVR_CASE_PAGE } from "@/components/features/cve-radar/cases/case.page";

export default function CvrCaseListPage() {
  return <EntityListPage page={CVR_CASE_PAGE} />;
}
