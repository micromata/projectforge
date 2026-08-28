"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session,
 * and the list put it there before it routed here (see MassUpdatePage).
 */
export default function TimesheetMassUpdatePage() {
  const massUpdate = TIMESHEET_PAGE.massUpdate!;
  return (
    <MassUpdatePage
      entity={TIMESHEET_PAGE.entity}
      massUpdate={massUpdate}
      listRoute={TIMESHEET_PAGE.route}
      // Built here rather than inside the generic page, because it renders the timesheet list's own
      // columns — and those are typed, so only the page that declares them can pass them on.
      selectedEntries={(count) => (
        <SelectedEntriesPanel
          endpoint={massUpdate.endpoint}
          metadata={TIMESHEET_PAGE.metadata}
          columns={TIMESHEET_PAGE.columns}
          // The count is all this page knows of the selection, and it comes from `{page}/meta`, which is
          // refetched on every visit — so a selection changed elsewhere refetches the rows with it.
          selectionKey={String(count)}
          count={count}
        />
      )}
    />
  );
}
