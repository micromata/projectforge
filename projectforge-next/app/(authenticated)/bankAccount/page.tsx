"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { BANK_ACCOUNT_PAGE } from "@/components/features/bank-account/bank-account.page";

export default function BankAccountListPage() {
  return <EntityListPage page={BANK_ACCOUNT_PAGE} />;
}
