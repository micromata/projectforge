"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { ACCOUNT_PAGE } from "@/components/features/account/account.page";

export default function AccountListPage() {
  return <EntityListPage page={ACCOUNT_PAGE} />;
}
