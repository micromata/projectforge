"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { COST1_PAGE } from "@/components/features/cost1/cost1.page";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session,
 * and the list put it there before it routed here (see MassUpdatePage).
 */
export default function Cost1MassUpdatePage() {
  const massUpdate = COST1_PAGE.massUpdate!;
  return (
    <MassUpdatePage
      entity={COST1_PAGE.entity}
      massUpdate={massUpdate}
      listRoute={COST1_PAGE.route}
      listQueryKey={COST1_PAGE.queryKey}
      // Built here rather than inside the generic page, because it renders the cost 1 list's own
      // columns — and those are typed, so only the page that declares them can pass them on.
      selectedEntries={(count) => (
        <SelectedEntriesPanel
          endpoint={massUpdate.endpoint}
          metadata={COST1_PAGE.metadata}
          columns={COST1_PAGE.columns}
          // The count is all this page knows of the selection, and it comes from `{page}/meta`, which
          // is refetched on every visit — so a selection changed elsewhere refetches the rows with it.
          selectionKey={String(count)}
          count={count}
        />
      )}
    />
  );
}
