import {
  TASK_ROUTE,
  TASK_TREE_ROUTE,
} from "@/components/shared/tasks/task-routes";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import { definePage } from "@/lib/page-def/define-page";
import { FinanceSection } from "./edit/finance-section";
import { taskSchema, TASK_FIELDS, type TaskValues } from "./task-schema";
import { emptyTaskValues, toFormValues } from "./task-values";
import {
  TASK_NEW_ENTRY_PARAMS,
  type TaskDetail,
  type TaskListRow,
} from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const TASK_LIST_QUERY_KEY = ["task"] as const;

/**
 * The task page as data (see lib/page-def/types.ts) — the form of one structure element.
 *
 * The field inventory and its order are `TaskEditForm`'s; every label, every rule and the constants of
 * the four enums come from `TaskDO` through the generated metadata, including the bounds of `progress`,
 * `maxHours` and `duration` (`@PropertyInfo(min/max)`). Two of the three sections start folded, as the
 * two closed `ToggleContainerPanel`s of the Wicket form do.
 *
 * Not on the Wicket form and therefore not here: `workpackageCode` (held in the values so a save does
 * not erase it, see task-schema.ts) and the computed `consumption`.
 *
 * The columns are a placeholder: the list is step 4 of projectforge-next/MIGRATION.md, and until then
 * `/task` has no page of its own — `route` is what `${route}/${id}` is built from. A task is reached
 * from the tree, which is why `returnTargets` names it (step 4 prepends the list).
 */
export const TASK_PAGE = definePage<
  TaskListRow,
  TaskValues,
  TaskDetail,
  typeof TASK_METADATA
>({
  entity: "task",
  metadata: TASK_METADATA,
  route: TASK_ROUTE,
  queryKey: TASK_LIST_QUERY_KEY,
  categoryKey: "menu.taskTree",
  titleKey: "task.title.list",
  columns: [{ name: "title", size: 400 }],
  edit: {
    schema: taskSchema,
    fieldNames: TASK_FIELDS,
    defaultValues: emptyTaskValues,
    toFormValues,
    title: (task) => task.title ?? "",
    newTitleKey: "task.title.add",
    savedMessageKey: "message.successfullChanged",
    returnTargets: [{ route: TASK_TREE_ROUTE, labelKey: "menu.taskTree" }],
    // "Add a subtask" from the tree: the parent is a parameter of the preset, because only the backend
    // can resolve what hangs on it — the project of the cost unit block, and the rights of the two
    // access-gated groups (see TaskPagesRest.newBaseDO and useNewEntryParams).
    newEntryParams: TASK_NEW_ENTRY_PARAMS,
    sections: [
      {
        id: "general",
        titleKey: "task.title.heading",
        fields: [
          { name: "parentTask" },
          { name: "title", emphasized: true },
          { name: "status" },
          { name: "responsibleUser" },
          { name: "priority" },
          {
            name: "maxHours",
            maxDigits: 4,
            hintKey: "task.edit.maxHoursIngoredDueToAssignedOrders",
          },
          { name: "shortDescription", span: 3 },
          { name: "reference", span: 3 },
          // Last, as it is in Wicket — where it is a panel of its own, which here would be a card
          // holding nothing but one textarea.
          { name: "description", span: 3, rows: 6 },
        ],
      },
      {
        id: "gantt",
        titleKey: "task.gantt.settings",
        collapsed: true,
        fields: [
          { name: "ganttObjectType" },
          { name: "startDate" },
          { name: "endDate" },
          // The units Wicket writes behind the boxes (`FieldsetPanel.setUnit`), here as a hint:
          // NumberField's suffix slot is the currency of an amount. `duration` gets none, as in
          // Wicket — its label already reads "Dauer" and the value is days by definition.
          { name: "progress", maxDigits: 3, hintKey: "percent" },
          { name: "duration", maxDigits: 5 },
          { name: "ganttPredecessorOffset", maxDigits: 5, hintKey: "days" },
          { name: "ganttRelationType" },
          { name: "ganttPredecessor" },
        ],
      },
      {
        id: "financeAdministration",
        titleKey: "financeAdministration",
        collapsed: true,
        // Hand-rendered: five fields whose writability is the user's access, and the cost unit block,
        // whose preview only the backend can compute (see FinanceSection).
        render: ({ id }) => <FinanceSection id={id} />,
      },
    ],
  },
});
