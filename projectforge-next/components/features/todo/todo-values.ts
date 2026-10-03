import type { ToDoValues } from "./todo-schema";
import type { ToDoDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 *
 * The notification starts checked wherever mail can be sent, as the Wicket form had it; the backend
 * notifies on a new to-do and on a changed assignee, status or deletion state anyhow.
 */
export function toFormValues(todo: ToDoDetail): ToDoValues {
  return {
    id: todo.id ?? null,
    subject: todo.subject ?? "",
    type: todo.type ?? null,
    status: todo.status ?? null,
    priority: todo.priority ?? null,
    dueDate: todo.dueDate ?? null,
    assignee: todo.assignee ?? null,
    reporter: todo.reporter ?? null,
    task: todo.task ?? null,
    group: todo.group ?? null,
    resubmission: todo.resubmission ?? null,
    description: todo.description ?? null,
    comment: todo.comment ?? null,
    sendNotification: todo.mailConfigured === true,
  };
}

/**
 * Blank form for a to-do that doesn't exist yet. Reporter, status, type and priority are preset by the
 * backend (`ToDoEntityRest.newBaseDO`), whose `/rs/todo/newEntry` answer the form is reset onto.
 */
export function emptyToDoValues(): ToDoValues {
  return toFormValues({ id: null });
}

/**
 * The fields a template fills: everything it holds but null, which means "not part of the template" —
 * the form keeps its own value there (see ToDoFavorite).
 */
export function templateFieldsOf(template: ToDoDetail): Partial<ToDoValues> {
  const fields: Partial<ToDoValues> = {
    subject: template.subject ?? undefined,
    type: template.type ?? undefined,
    status: template.status ?? undefined,
    priority: template.priority ?? undefined,
    assignee: template.assignee ?? undefined,
    reporter: template.reporter ?? undefined,
    task: template.task ?? undefined,
    group: template.group ?? undefined,
    description: template.description ?? undefined,
    comment: template.comment ?? undefined,
  };
  return Object.fromEntries(
    Object.entries(fields).filter(([, value]) => value !== undefined)
  ) as Partial<ToDoValues>;
}
