import { GROUP_TASK_ACCESS_METADATA } from "@/lib/metadata/group-task-access.generated";
import { definePage } from "@/lib/page-def/define-page";
import { accessSchema, ACCESS_FIELDS, type AccessValues } from "./schema";
import { emptyAccessValues, toFormValues } from "./values";
import { GroupTaskFields } from "./edit/group-task-fields";
import { TemplateButtons } from "./edit/template-buttons";
import { AccessMatrix } from "./edit/access-matrix";
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
  columns: [
    {
      // The structure element the rights apply to. A computed column: the DTO carries the task as an
      // id-only reference with a title, not a field the list could sort by that name.
      id: "task",
      labelKey: "task",
      accessor: (row) => row.task?.title ?? row.task?.displayName ?? null,
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
    { name: "description", size: 320, wrap: true },
  ],
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
