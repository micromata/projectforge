"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { AVAILABILITY_PAGE } from "@/components/features/availability/availability.page";

export default function AvailabilityListPage() {
  return <EntityListPage page={AVAILABILITY_PAGE} />;
}
