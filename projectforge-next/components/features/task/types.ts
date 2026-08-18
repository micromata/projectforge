// Mirrors org.projectforge.rest.dto.Task (projectforge-rest). Keep the field names in sync with that
// DTO — they are what `copyFrom`/`copyTo` read and write, not `TaskDO`'s.
//
// Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
// (JacksonConfiguration), so an empty field is absent from the JSON rather than null. `toFormValues`
// normalises that away (see task-values.ts).

import type { TASK_METADATA } from "@/lib/metadata/task.generated";

type EnumOf<F extends { enumValues?: readonly { value: string }[] }> =
  NonNullable<F["enumValues"]>[number]["value"];

export type TaskStatus = EnumOf<typeof TASK_METADATA.fields.status>;
export type TaskPriority = EnumOf<typeof TASK_METADATA.fields.priority>;
export type TimesheetBookingStatus = EnumOf<
  typeof TASK_METADATA.fields.timesheetBookingStatus
>;
export type GanttObjectType = EnumOf<
  typeof TASK_METADATA.fields.ganttObjectType
>;
export type GanttRelationType = EnumOf<
  typeof TASK_METADATA.fields.ganttRelationType
>;

/**
 * A referenced entity as the DTO carries it: the id to write back, the name to show. A type alias
 * rather than an interface, so it satisfies the index signature of the schema's `looseObject` (see
 * `entityField` in from-metadata.ts).
 */
export type EntityRefDto = {
  id: number;
  displayName?: string;
};

/**
 * The task as `GET /rs/task/{id}` answers it.
 *
 * The dates are `LocalDate` on the DTO and travel as `yyyy-MM-dd` strings; the four access flags are
 * `EntityAccessSupport` plus the two the task adds — see `Task.kt` for which rule each stands for.
 */
export interface TaskDetail {
  /** null for a task that has not been saved yet (Spring assigns the id). */
  id: number | null;
  parentTask?: EntityRefDto | null;
  title?: string | null;
  status?: TaskStatus | null;
  priority?: TaskPriority | null;
  shortDescription?: string | null;
  description?: string | null;
  progress?: number | null;
  maxHours?: number | null;
  startDate?: string | null;
  endDate?: string | null;
  duration?: number | null;
  protectTimesheetsUntil?: string | null;
  responsibleUser?: EntityRefDto | null;
  reference?: string | null;
  timesheetBookingStatus?: TimesheetBookingStatus | null;
  /**
   * Held but never shown: `TaskDO` has the column, but no form ever offered a box for it — neither
   * `TaskEditForm` nor the legacy React page — so this page doesn't either. It still travels back
   * untouched, because a key Spring doesn't receive leaves the DTO's field null and `Task.copyTo`
   * would write that over the stored text.
   */
  workpackageCode?: string | null;
  kost2BlackWhiteList?: string | null;
  kost2IsBlackList?: boolean | null;
  protectionOfPrivacy?: boolean | null;
  ganttPredecessorOffset?: number | null;
  ganttRelationType?: GanttRelationType | null;
  ganttObjectType?: GanttObjectType | null;
  ganttPredecessor?: EntityRefDto | null;
  /** May this user change the task at all (`EntityAccessSupport`, filled by `AbstractEntityRest.getById`). */
  writeAccess?: boolean | null;
  deleteAccess?: boolean | null;
  /** `kost2BlackWhiteList`, `kost2IsBlackList` and `timesheetBookingStatus` — see FinanceSection. */
  kost2AndBookingStatusWriteAccess?: boolean | null;
  /** `protectTimesheetsUntil` and `protectionOfPrivacy` — the finance group only. */
  protectTimesheetsUntilWriteAccess?: boolean | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/**
 * Projection the list page renders — the same DTO, with the id the table keys rows by.
 *
 * The list itself is not migrated yet (step 4 of projectforge-next/MIGRATION.md); the tree is what
 * shows tasks today.
 */
export interface TaskListRow extends TaskDetail {
  id: number;
}
