"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { TASK_PAGE } from "@/components/features/task/task.page";

export default function TaskListPage() {
  return <EntityListPage page={TASK_PAGE} />;
}
