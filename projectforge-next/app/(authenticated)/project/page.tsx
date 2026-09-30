"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { PROJECT_PAGE } from "@/components/features/project/project.page";

export default function ProjectListPage() {
  return <EntityListPage page={PROJECT_PAGE} />;
}
