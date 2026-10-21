"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { BANK_ACCOUNT_RECORD_PAGE } from "@/components/features/bank-account-record/bank-account-record.page";
import { BANK_ACCOUNT_ID_PARAM } from "@/components/features/bank-account/bank-account-links";
import type { MagicFilter } from "@/lib/rs/types";

export default function BankAccountRecordListPage() {
  return (
    // `useSearchParams` needs this boundary under `output: "export"`; the first, empty read is just the
    // list with its remembered filter (no account jump).
    <Suspense fallback={<EntityListPage page={BANK_ACCOUNT_RECORD_PAGE} />}>
      <BankAccountRecordListBody />
    </Suspense>
  );
}

/**
 * The transactions, optionally opened by a bank account's "transactions" jump (`?bankAccountId=…`), which seeds a
 * **transient** filter on that account — not merged with the remembered filter and not stored back afterwards.
 */
function BankAccountRecordListBody() {
  const bankAccountId = Number(useSearchParams().get(BANK_ACCOUNT_ID_PARAM));

  // The backend's `accounts` filter reads the ids as strings (BankAccountRecordEntityRest.preProcessMagicFilter).
  const filterOverride: MagicFilter | undefined =
    bankAccountId > 0
      ? {
          entries: [
            { field: "accounts", value: { values: [String(bankAccountId)] } },
          ],
          sortProperties: [],
        }
      : undefined;

  return (
    <EntityListPage
      page={BANK_ACCOUNT_RECORD_PAGE}
      filterOverride={filterOverride}
      transient={!!filterOverride}
    />
  );
}
