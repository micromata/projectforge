"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";
import { TaskKost2MassUpdateField } from "@/components/features/timesheet/mass-update/task-kost2-mass-update-field";

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
      // The task/cost-unit picker the generic renderer has no field for: the cost units follow the
      // task, which is a dependency a declared field cannot express (see MassUpdateForm.extraFields).
      extraFields={(setParam) => (
        <TaskKost2MassUpdateField setParam={setParam} />
      )}
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
