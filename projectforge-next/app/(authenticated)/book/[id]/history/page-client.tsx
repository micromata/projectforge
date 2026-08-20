"use client";

import { EntityTabRedirect } from "@/components/shared/edit/entity-tab-redirect";
import { HISTORY_TAB_ID } from "@/components/shared/edit/entity-tabs";

// The history used to be a page of its own; it is a tab of the edit page now (see EntityTabRedirect).
export function BookHistoryPageClient() {
  return (
    <EntityTabRedirect
      pattern="/book/[id]/history"
      route="/book"
      tab={HISTORY_TAB_ID}
    />
  );
}
