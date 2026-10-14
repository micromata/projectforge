"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { TEAM_CAL_PAGE } from "@/components/features/teamCal/teamCal.page";

export default function TeamCalListPage() {
  return <EntityListPage page={TEAM_CAL_PAGE} />;
}
