"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { GANTT_PAGE } from "@/components/features/gantt/gantt.page";

export default function GanttListPage() {
  return <EntityListPage page={GANTT_PAGE} />;
}
