"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { COST2_PAGE } from "@/components/features/cost2/cost2.page";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session,
 * and the list put it there before it routed here (see MassUpdatePage).
 */
export default function Cost2MassUpdatePage() {
  const massUpdate = COST2_PAGE.massUpdate!;
  return (
    <MassUpdatePage
      entity={COST2_PAGE.entity}
      massUpdate={massUpdate}
      listRoute={COST2_PAGE.route}
      listQueryKey={COST2_PAGE.queryKey}
      // Built here rather than inside the generic page, because it renders the cost 2 list's own
      // columns — and those are typed, so only the page that declares them can pass them on.
      selectedEntries={(count) => (
        <SelectedEntriesPanel
          endpoint={massUpdate.endpoint}
          metadata={COST2_PAGE.metadata}
          columns={COST2_PAGE.columns}
          // The count is all this page knows of the selection, and it comes from `{page}/meta`, which
          // is refetched on every visit — so a selection changed elsewhere refetches the rows with it.
          selectionKey={String(count)}
          count={count}
        />
      )}
    />
  );
}
