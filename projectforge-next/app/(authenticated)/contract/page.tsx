"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { CONTRACT_PAGE } from "@/components/features/contract/contract.page";

export default function ContractListPage() {
  return <EntityListPage page={CONTRACT_PAGE} />;
}
