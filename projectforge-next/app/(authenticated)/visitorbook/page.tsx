"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { VISITORBOOK_PAGE } from "@/components/features/visitorbook/visitorbook.page";

export default function VisitorbookListPage() {
  return <EntityListPage page={VISITORBOOK_PAGE} />;
}
