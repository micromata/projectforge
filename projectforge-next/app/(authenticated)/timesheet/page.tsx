"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import {
  TIMESHEET_TASK_ID_PARAM,
  TIMESHEET_TASK_NAME_PARAM,
} from "@/components/data-table/cells/consumption-cell";
import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";
import type { MagicFilter } from "@/lib/rs/types";

export default function TimesheetListPage() {
  return (
    // `useSearchParams` needs this boundary under `output: "export"`; the first, empty read is just the
    // list with its remembered filter (no task jump).
    <Suspense fallback={<EntityListPage page={TIMESHEET_PAGE} />}>
      <TimesheetListBody />
    </Suspense>
  );
}

/**
 * The time sheet list, optionally opened by a jump from a task's consumption bar
 * (`?taskId=…&taskName=…`, see consumption-cell.tsx). Such a jump seeds a **transient, cleared** filter —
 * only the task, not merged with the remembered filter and not stored back afterwards — the three things
 * Wicket's `ConsumptionBarPanel` did with `taskId`/`clear`/`storeFilter`.
 */
function TimesheetListBody() {
  const params = useSearchParams();
  const taskId = Number(params.get(TIMESHEET_TASK_ID_PARAM));
  const taskName = params.get(TIMESHEET_TASK_NAME_PARAM) ?? undefined;

  // Only a real task id opens the filtered view; anything else is the plain list.
  const filterOverride: MagicFilter | undefined =
    taskId > 0
      ? {
          // Just the task — the backend's `task` filter reads `value.id` and searches its sub-tasks too
          // (recursive defaults to true, as the legacy list). The name is for the filter pill.
          entries: [
            {
              field: "task",
              value: { id: taskId, displayName: taskName },
            },
          ],
          sortProperties: [],
        }
      : undefined;

  return (
    <EntityListPage
      page={TIMESHEET_PAGE}
      filterOverride={filterOverride}
      transient={!!filterOverride}
    />
  );
}
