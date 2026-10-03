import { TO_DO_METADATA } from "@/lib/metadata/to-do.generated";
import { definePage } from "@/lib/page-def/define-page";
import { toDoSchema, TO_DO_FIELDS, type ToDoValues } from "./todo-schema";
import { ToDoPriorityCell } from "./todo-priority-cell";
import { ToDoSaveOption } from "./todo-save-option";
import { ToDoTemplatesBar } from "./todo-templates-bar";
import { emptyToDoValues, toFormValues } from "./todo-values";
import type { ToDoDetail, ToDoListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const TO_DO_LIST_QUERY_KEY = ["todo"] as const;

/**
 * The whole to-do page of the todo plugin — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the removed Wicket pages, which it follows: the list shows `ToDoListPage`'s columns in its
 * order and highlights the to-dos of the user with changes not seen yet; the form has `ToDoEditForm`'s
 * fields (and the resubmission date of the entity, which Wicket never offered), its templates, its
 * notification checkbox and its close button. Labels and every rule come from ToDoDO through the
 * generated metadata.
 */
export const TO_DO_PAGE = definePage<
  ToDoListRow,
  ToDoValues,
  ToDoDetail,
  typeof TO_DO_METADATA
>({
  entity: "todo",
  metadata: TO_DO_METADATA,
  route: "/todo",
  queryKey: TO_DO_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: Misc (ToDoPlugin, MenuItemDefId.MISC).
  categoryKey: "menu.misc",
  titleKey: "plugins.todo.title.list",
  columns: [
    { name: "created", size: 130 },
    { name: "lastUpdate", size: 130 },
    { name: "subject", size: 260, className: "font-semibold" },
    {
      name: "assignee",
      size: 150,
      cell: ({ row }) => row.original.assignee?.displayName ?? null,
    },
    {
      name: "reporter",
      size: 150,
      cell: ({ row }) => row.original.reporter?.displayName ?? null,
    },
    { name: "dueDate", size: 110 },
    { name: "status", size: 120 },
    {
      name: "priority",
      size: 100,
      cell: ({ row }) => <ToDoPriorityCell priority={row.original.priority} />,
    },
    { name: "type", size: 120 },
    {
      name: "task",
      size: 180,
      // The plain task title with the path to the root as the tooltip, as the time sheet list shows it.
      cell: ({ row }) =>
        row.original.task?.title ?? row.original.task?.displayName ?? null,
      tooltip: (row) => row.task?.path ?? undefined,
    },
    {
      name: "group",
      size: 140,
      cell: ({ row }) => row.original.group?.displayName ?? null,
    },
    { name: "description", size: 300, wrap: true },
  ],
  // Wicket's IMPORTANT_ROW: the to-dos of the user changed by somebody else, not opened since.
  legend: [{ className: "row-red", labelKey: "plugins.todo.legend.recent" }],
  // Not for a deleted to-do, which reads as deleted only (as on the Wicket list).
  rowClassName: (row) =>
    row.recentForMe && !row.deleted ? "row-red" : undefined,
  // The Wicket list's order: the latest change first.
  defaultSort: { id: "lastUpdate", desc: true },
  // Mass update of status, priority, type, assignee, dates, task, group and texts (ToDoMultiSelectedPageRest).
  massUpdate: {
    endpoint: "todoSelected",
    route: "/todo/mass-update",
  },
  edit: {
    schema: toDoSchema,
    fieldNames: TO_DO_FIELDS,
    defaultValues: emptyToDoValues,
    toFormValues,
    title: (todo) => todo.subject ?? "",
    newTitleKey: "plugins.todo.title.add",
    savedMessageKey: "message.successfullChanged",
    actions: ["close"],
    editIntro: ToDoTemplatesBar,
    saveOption: ToDoSaveOption,
    sections: [
      {
        id: "general",
        titleKey: "plugins.todo.todo",
        fields: [
          { name: "subject", span: 3 },
          { name: "type" },
          { name: "status" },
          { name: "priority" },
          { name: "assignee" },
          { name: "reporter" },
          { name: "dueDate" },
          {
            name: "task",
            span: 2,
            hintKey: "plugins.todo.task.tooltip.content",
          },
          { name: "group", hintKey: "plugins.todo.group.tooltip.content" },
          { name: "resubmission" },
          { name: "description", span: 3, rows: 4 },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
