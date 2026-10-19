"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { LEAVE_ACCOUNT_ENTRY_PAGE } from "@/components/features/leave-account-entry/leave-account-entry.page";

export default function LeaveAccountEntryListPage() {
  return <EntityListPage page={LEAVE_ACCOUNT_ENTRY_PAGE} />;
}
