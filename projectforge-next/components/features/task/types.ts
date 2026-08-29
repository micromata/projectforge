// Mirrors org.projectforge.rest.dto.Task (projectforge-rest). Keep the field names in sync with that
// DTO — they are what `copyFrom`/`copyTo` read and write, not `TaskDO`'s.
//
// Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
// (JacksonConfiguration), so an empty field is absent from the JSON rather than null. `toFormValues`
// normalises that away (see task-values.ts).

import type { TASK_METADATA } from "@/lib/metadata/task.generated";
import type { TaskConsumption, TaskOrder } from "@/lib/rs/task";

/**
 * Parameters of `/task/new` the backend reads for its preset: the parent of a new subtask, which
 * `TaskPagesRest.newBaseDO` resolves (and with it the project the cost unit block needs).
 *
 * Here rather than beside the page declaration, because the declaration and every section that reads
 * the preset back out of the cache need the same list — the parameters are part of the query key, so a
 * second read spelling them differently would be a second request (see useEntityDetail). A section is
 * imported *by* the declaration, so the shared constant cannot live there.
 */
export const TASK_NEW_ENTRY_PARAMS = ["parentTaskId"] as const;

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
 * A row of the list, as `Task.copyFrom4ListRow` fills it — the ten columns of `TaskListPage` and
 * nothing else.
 *
 * Not `TaskDetail`: the lean row deliberately omits `description`, `kost2BlackWhiteList`, the access
 * flags and the nested `parentTask`, so a column reading one of those would be an empty column. The
 * three values that are not on `TaskDO` are computed per row from the in-memory tree, the same
 * functions the tree perspective calls.
 */
export interface TaskListRow {
  id: number;
  title?: string | null;
  /** The path to the root ("A -> B -> C") including this task — the tooltip of the title column. */
  path?: string | null;
  /** Whether the task is marked as deleted — the row is then tinted and struck through. */
  deleted?: boolean;
  shortDescription?: string | null;
  /** `LocalDate` on the DTO, travelling as `yyyy-MM-dd`. */
  protectTimesheetsUntil?: string | null;
  reference?: string | null;
  priority?: TaskPriority | null;
  status?: TaskStatus | null;
  responsibleUser?: EntityRefDto | null;
  consumption?: TaskConsumption | null;
  /** e.g. `6.000.00.*` — the shared prefix of the task's cost units. */
  kost2WildCard?: string | null;
  /** All cost 2 numbers, one per line — the kost2 column's tooltip. */
  kost2ListAsLines?: string | null;
  orderList?: TaskOrder[] | null;
  created?: string | null;
  lastUpdate?: string | null;
}
