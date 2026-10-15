"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { TO_DO_PAGE } from "@/components/features/todo/todo.page";

export default function ToDoListPage() {
  return <EntityListPage page={TO_DO_PAGE} />;
}
