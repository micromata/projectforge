// Mirrors org.projectforge.plugins.todo.dto.ToDo (todo plugin). Keep field names in sync with the Spring
// DTO — `recent`, `recentForMe`, `sendNotification` and `mailConfigured` have no counterpart in the
// generated metadata.

import type { TO_DO_METADATA } from "@/lib/metadata/to-do.generated";

/** The constants of org.projectforge.plugins.todo.ToDoStatus, from the metadata. */
export type ToDoStatus =
  (typeof TO_DO_METADATA.fields.status.enumValues)[number]["value"];
export type ToDoType =
  (typeof TO_DO_METADATA.fields.type.enumValues)[number]["value"];
export type Priority =
  (typeof TO_DO_METADATA.fields.priority.enumValues)[number]["value"];

/**
 * A referenced object as the DTO carries it: the id to write back, the name to show. A type alias rather
 * than an interface, so it satisfies the index signature of the schema's `looseObject`.
 */
export type RefDto = {
  id: number;
  displayName?: string;
};

/** The task of a to-do: the title for the list, the path to the root for its tooltip. */
export type ToDoTaskDto = RefDto & {
  title?: string | null;
  path?: string | null;
};

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses
 * `JsonInclude.Include.NON_NULL` (JacksonConfiguration), so an empty field is absent from the JSON
 * rather than null. `toFormValues` normalises that away.
 */
export interface ToDoDetail {
  /** null for a to-do that has not been saved yet (Spring assigns the id). */
  id: number | null;
  subject?: string | null;
  reporter?: RefDto | null;
  assignee?: RefDto | null;
  task?: ToDoTaskDto | null;
  group?: RefDto | null;
  description?: string | null;
  comment?: string | null;
  type?: ToDoType | null;
  status?: ToDoStatus | null;
  priority?: Priority | null;
  dueDate?: string | null;
  resubmission?: string | null;
  /** A change of somebody else the assignee hasn't seen yet. Read-only, see `ToDoDO.recent`. */
  recent?: boolean;
  /** [recent], and the logged-in user is the assignee: the rows the list highlights. Read-only. */
  recentForMe?: boolean;
  /** Whether the save notifies assignee and reporter by e-mail — the checkbox beside the save button. */
  sendNotification?: boolean;
  /** Whether e-mails can be sent at all, so the checkbox is offered only then. Read-only. */
  mailConfigured?: boolean;
  deleted?: boolean;
  writeAccess?: boolean;
  deleteAccess?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface ToDoListRow extends ToDoDetail {
  id: number;
}
