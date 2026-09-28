// Mirrors org.projectforge.rest.dto.GroupTaskAccess (projectforge-rest). The permission matrix is a
// list of AccessEntry, one per AccessType; the group and task are carried as references the DO
// serializes id-only.

import {
  Delete02Icon,
  Download01Icon,
  Edit02Icon,
  File01Icon,
} from "@hugeicons/core-free-icons";

/**
 * The four access types of the permission matrix, in the order the Wicket page shows them
 * (`GroupTaskAccessDO.orderedEntries`). The enum is serialized as its name (`@Enumerated(STRING)`), so
 * these are exactly the strings on the wire.
 */
export const ACCESS_TYPES = [
  "TASK_ACCESS_MANAGEMENT",
  "TASKS",
  "TIMESHEETS",
  "OWN_TIMESHEETS",
] as const;

export type AccessType = (typeof ACCESS_TYPES)[number];

/** The i18n key of an access type's row label — the suffix is the enum's `getKey()`. */
export const ACCESS_TYPE_LABEL_KEY: Record<AccessType, string> = {
  TASK_ACCESS_MANAGEMENT: "access.type.accessManagement",
  TASKS: "access.type.tasks",
  TIMESHEETS: "access.type.timesheets",
  OWN_TIMESHEETS: "access.type.ownTimesheets",
};

/**
 * The four SQL operations of a row, in the order the Wicket table shows the columns — with the
 * Hugeicon each one is labelled by (document / download / edit / delete), which the list matrix cell
 * draws as its column header and the edit matrix names its columns with. `key` is exactly the
 * `AccessEntryDto` field it toggles, so a cell binds to it without a lookup table.
 */
export const OPERATIONS = [
  { key: "accessSelect", labelKey: "access.type.select", icon: File01Icon },
  { key: "accessInsert", labelKey: "access.type.insert", icon: Download01Icon },
  { key: "accessUpdate", labelKey: "access.type.update", icon: Edit02Icon },
  { key: "accessDelete", labelKey: "access.type.delete", icon: Delete02Icon },
] as const;

/** One row of the matrix: an access type with its four operation flags. */
export interface AccessEntryDto {
  accessType: AccessType;
  accessSelect: boolean;
  accessInsert: boolean;
  accessUpdate: boolean;
  accessDelete: boolean;
}

/**
 * A referenced group as the DTO carries it — the id to write back (`GroupTaskAccess.copyTo` resolves
 * the `GroupDO` by it), the name to show. `name` is the group's own column value; `displayName` is what
 * the autocomplete binds to.
 */
export type GroupRefDto = {
  id: number;
  displayName?: string;
  name?: string;
  /** Whether the referenced group is soft-deleted — the group column strikes it through when it is. */
  deleted?: boolean;
};

/**
 * A referenced task; `title` is its list column value, `displayName` its autocomplete label, `path` the
 * path to the root ("Micromata -> Business Unit -> ProjectForge") shown as the column's tooltip.
 */
export type TaskRefDto = {
  id: number;
  displayName?: string;
  title?: string;
  path?: string;
  /** Whether the referenced task is soft-deleted — the task column strikes it through when it is. */
  deleted?: boolean;
};

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface AccessDetail {
  /** null for an access entry that has not been saved yet (Spring assigns the id). */
  id: number | null;
  group?: GroupRefDto | null;
  task?: TaskRefDto | null;
  /** Whether the rights also apply to every sub structure element; defaults to true. */
  recursive?: boolean | null;
  description?: string | null;
  accessEntries?: AccessEntryDto[] | null;
  /** Whether the logged-in user may save this entry (EntityAccessSupport); absent means allowed. */
  writeAccess?: boolean | null;
  deleteAccess?: boolean | null;
  /** `boolean` (not `| null`): NON_NULL omits it for a row that isn't deleted, so it matches ListRow. */
  deleted?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface AccessListRow extends AccessDetail {
  id: number;
}
