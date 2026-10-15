"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { CONFIGURATION_PAGE } from "@/components/features/configuration/configuration.page";

export default function ConfigurationListPage() {
  return <EntityListPage page={CONFIGURATION_PAGE} />;
}
