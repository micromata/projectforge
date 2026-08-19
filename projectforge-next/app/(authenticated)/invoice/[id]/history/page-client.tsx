"use client";

import { EntityTabRedirect } from "@/components/shared/edit/entity-tab-redirect";
import { HISTORY_TAB_ID } from "@/components/shared/edit/entity-tabs";

// The history used to be a page of its own; it is a tab of the edit page now (see EntityTabRedirect).
export function InvoiceHistoryPageClient() {
  return (
    <EntityTabRedirect
      pattern="/invoice/[id]/history"
      route="/invoice"
      tab={HISTORY_TAB_ID}
    />
  );
}
