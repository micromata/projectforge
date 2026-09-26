import { GROUP_TASK_ACCESS_METADATA } from "@/lib/metadata/group-task-access.generated";
import { definePage } from "@/lib/page-def/define-page";
import { accessSchema, ACCESS_FIELDS, type AccessValues } from "./schema";
import { emptyAccessValues, toFormValues } from "./values";
import { GroupTaskFields } from "./edit/group-task-fields";
import { TemplateButtons } from "./edit/template-buttons";
import { AccessMatrix } from "./edit/access-matrix";
import { AccessMatrixCell } from "./list/access-matrix-cell";
import { AccessListActions } from "./list/access-list-actions";
import type { AccessDetail, AccessListRow } from "./types";

/** REST category of the access-rights entity — `GroupAccessEntityRest` is mapped to "access". */
export const ACCESS_ENTITY = "access";
/** React Query key of the list, so a write from the edit page refreshes it. */
export const ACCESS_LIST_QUERY_KEY = ["access"] as const;
/** Route of the list; the edit page hangs below it as `/access/{id}`. */
export const ACCESS_ROUTE = "/access";

/**
 * The access-rights management page — list and edit — as data (see lib/page-def/types.ts), the
 * successor of the Wicket `AccessListPage` / `AccessEditPage`. Each `GroupTaskAccessDO` grants one group
 * a set of permissions on one structure element (task), the permissions held as the four-by-four matrix
 * of [AccessMatrix].
 *
 * Hand-built rather than server-laid-out because the matrix is a fixed-shape collection no `UILayout`
 * describes (the legacy generic React `AccessTableComponent` was a dead stub of unbound checkboxes).
 * Group and task are picked through custom autocomplete fields ([GroupTaskFields]); `recursive` and
 * `description` come from GroupTaskAccessDO through the generated metadata.
 */
export const ACCESS_PAGE = definePage<
  AccessListRow,
  AccessValues,
  AccessDetail,
  typeof GROUP_TASK_ACCESS_METADATA
>({
  entity: ACCESS_ENTITY,
  metadata: GROUP_TASK_ACCESS_METADATA,
  route: ACCESS_ROUTE,
  queryKey: ACCESS_LIST_QUERY_KEY,
  // Administration > Access management (MenuCreator, ACCESS_LIST).
  categoryKey: "menu.administration",
  titleKey: "access.title.list",
  // Sort by the structure element in task-tree order (GroupAccessEntityRest.computedSortProperties), so
  // the rows arrive as a tree and the indented task column below reads as one.
  defaultSort: { id: "task" },
  columns: [
    {
      // The structure element the rights apply to. A computed column: the DTO carries the task as an
      // id-only reference with a title, not a field the list could sort by that name. The path to the
      // root is the tooltip — the same as the time sheet list's task column, kept consistent.
      //
      // Indented by the task's depth in the tree (its path's segment count) so the list, sorted in tree
      // order by default, reads as a tree — the 0.9rem step matches the /taskTree view (TreeCell). Only
      // tasks that carry a grant appear, so an ancestor without one leaves a gap rather than a row.
      id: "task",
      labelKey: "task",
      accessor: (row) => row.task?.title ?? row.task?.displayName ?? null,
      tooltip: (row) => row.task?.path ?? undefined,
      cell: (ctx) => {
        const task = ctx.row.original.task;
        const depth = task?.path ? task.path.split(" -> ").length - 1 : 0;
        return (
          <span
            className="font-medium"
            style={{ paddingLeft: `${depth * 0.9}rem` }}
          >
            {task?.title ?? task?.displayName ?? null}
          </span>
        );
      },
      size: 320,
    },
    {
      // The group the rights are granted to — likewise a reference carried on the DTO.
      id: "group",
      labelKey: "group",
      accessor: (row) => row.group?.name ?? row.group?.displayName ?? null,
      size: 260,
    },
    // Whether the rights also cover every sub structure element (GroupTaskAccessDO.recursive).
    { name: "recursive", size: 110 },
    {
      // The permission matrix per row, as the Wicket list drew it (AccessTablePanel). A computed
      // column: `accessEntries` is a fixed-shape collection no single property backs, so it neither
      // sorts nor filters (see ColumnBase.sortable/filterKind).
      id: "accessEntries",
      labelKey: "access.type",
      accessor: (row) => row.accessEntries,
      sortable: false,
      filterKind: null,
      size: 190,
      cell: (ctx) => (
        <AccessMatrixCell entries={ctx.row.original.accessEntries} />
      ),
    },
    { name: "description", size: 320, wrap: true },
  ],
  // The task ("structure") wizard, for admins — the "Assistent" of the Wicket list.
  listActions: AccessListActions,
  edit: {
    schema: accessSchema,
    fieldNames: ACCESS_FIELDS,
    arrayFieldNames: ["accessEntries"],
    defaultValues: emptyAccessValues,
    toFormValues,
    // How an entry is referred to: the group on the structure element it is granted for.
    title: (entry) => {
      const group = entry.group?.displayName ?? entry.group?.name ?? "";
      const task = entry.task?.displayName ?? entry.task?.title ?? "";
      return [group, task].filter(Boolean).join(" – ");
    },
    newTitleKey: "access.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "access",
        titleKey: "access.title.heading",
        fields: [
          { custom: GroupTaskFields, span: 3 },
          {
            name: "recursive",
            hintKey: "access.recursive.help",
          },
          { custom: TemplateButtons, span: 3 },
          { custom: AccessMatrix, span: 3 },
          { name: "description", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
