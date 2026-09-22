"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { EntityListPage } from "@/components/shared/list/entity-list-page";
import {
  TIMESHEET_END_DATE_PARAM,
  TIMESHEET_KOST2_ID_PARAM,
  TIMESHEET_KOST2_LABEL_PARAM,
  TIMESHEET_START_DATE_PARAM,
  TIMESHEET_TASK_ID_PARAM,
  TIMESHEET_TASK_NAME_PARAM,
  TIMESHEET_USER_ID_PARAM,
  TIMESHEET_USER_NAME_PARAM,
} from "@/lib/timesheet-links";
import { TIMESHEET_PAGE } from "@/components/features/timesheet/timesheet.page";
import type { MagicFilter, MagicFilterEntry } from "@/lib/rs/types";

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
 * The time sheet list, optionally opened by a jump that seeds a **transient, cleared** filter — not merged
 * with the remembered filter and not stored back afterwards, the three things Wicket did with
 * `clear`/`storeFilter`. Two jumps land here:
 * - a task's consumption bar (`?taskId=…&taskName=…`, see consumption-cell.tsx) — just the task;
 * - a monthly-employee-report row (see lib/timesheet-links.ts) — the reported user, the exact cost unit
 *   (`kost2.id`) or task, and the month range (`?userId=…&kost2Id=…|taskId=…&startDate=…&endDate=…`).
 */
function TimesheetListBody() {
  const params = useSearchParams();
  const taskId = Number(params.get(TIMESHEET_TASK_ID_PARAM));
  const taskName = params.get(TIMESHEET_TASK_NAME_PARAM) ?? undefined;
  const userId = Number(params.get(TIMESHEET_USER_ID_PARAM));
  const userName = params.get(TIMESHEET_USER_NAME_PARAM) ?? undefined;
  const kost2Id = Number(params.get(TIMESHEET_KOST2_ID_PARAM));
  const kost2Label = params.get(TIMESHEET_KOST2_LABEL_PARAM) ?? undefined;
  const startDate = params.get(TIMESHEET_START_DATE_PARAM) ?? undefined;
  const endDate = params.get(TIMESHEET_END_DATE_PARAM) ?? undefined;

  const entries: MagicFilterEntry[] = [];
  if (userId > 0) {
    // The backend's `user` filter reads `value.id`; the name is for the filter pill.
    entries.push({
      field: "user",
      value: { id: userId, displayName: userName },
    });
  }
  if (startDate) {
    // The DATE range picker sends day-only bounds as `from`/`to`; the backend widens them to whole days
    // and matches by overlap (see TimesheetPagesRest.preProcessMagicFilter).
    entries.push({ field: "period", value: { from: startDate, to: endDate } });
  }
  if (kost2Id > 0) {
    // Exact cost unit drill-down (`kost2.id`), not a number search — the backend runs the same DB query
    // the report total does. The label is for the filter pill.
    entries.push({
      field: "kost2.id",
      value: { id: kost2Id, displayName: kost2Label },
    });
  } else if (taskId > 0) {
    // The `task` filter reads `value.id` and searches its sub-tasks too (recursive defaults to true).
    entries.push({
      field: "task",
      value: { id: taskId, displayName: taskName },
    });
  }

  const filterOverride: MagicFilter | undefined =
    entries.length > 0 ? { entries, sortProperties: [] } : undefined;

  return (
    <EntityListPage
      page={TIMESHEET_PAGE}
      filterOverride={filterOverride}
      transient={!!filterOverride}
    />
  );
}
