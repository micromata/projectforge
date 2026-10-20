"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { SKILL_ENTRY_PAGE } from "@/components/features/skill-entry/skill-entry.page";

export default function SkillEntryListPage() {
  return <EntityListPage page={SKILL_ENTRY_PAGE} />;
}
