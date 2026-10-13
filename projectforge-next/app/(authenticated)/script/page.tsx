"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { SCRIPT_PAGE } from "@/components/features/script/script.page";

export default function ScriptListPage() {
  return <EntityListPage page={SCRIPT_PAGE} />;
}
