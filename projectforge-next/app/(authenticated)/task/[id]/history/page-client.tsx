"use client";

import { EntityTabRedirect } from "@/components/shared/edit/entity-tab-redirect";
import { HISTORY_TAB_ID } from "@/components/shared/edit/entity-tabs";

// The history used to be a page of its own; it is a tab of the edit page now (see EntityTabRedirect).
// The `?returnTo=` the tree and the list sent along is dropped here on purpose: the redirect leads to
// the form, which is where a return target belongs (see useEditReturn), and a task reached through an
// old history link has no caller to go back to anyway.
export function TaskHistoryPageClient() {
  return (
    <EntityTabRedirect
      pattern="/task/[id]/history"
      route="/task"
      tab={HISTORY_TAB_ID}
    />
  );
}
