import {
  TASK_ROUTE,
  TASK_TREE_ROUTE,
  TASK_WIZARD_ROUTE,
  HIGHLIGHT_ID_PARAM,
  taskWizardHref,
} from "@/components/shared/tasks/task-routes";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import { definePage } from "@/lib/page-def/define-page";
import { JiraLinkedText } from "@/components/shared/jira/jira-linked-text";
import { makeJiraFieldLinks } from "@/components/shared/jira/jira-field-links";
import { FinanceSection } from "./edit/finance-section";
import { TaskListActions } from "./task-list-actions";
import {
  TaskConsumptionCell,
  TaskOrdersCell,
  TaskStatusListCell,
} from "./task-list-cells";
import { taskSchema, TASK_FIELDS, type TaskValues } from "./task-schema";
import { emptyTaskValues, toFormValues } from "./task-values";
import {
  TASK_NEW_ENTRY_PARAMS,
  type TaskDetail,
  type TaskListRow,
} from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const TASK_LIST_QUERY_KEY = ["task"] as const;

// JIRA issue links below the full-width free-text fields, as Wicket's `addJIRAField` shows them
// (TaskEditForm). Not below `title`, a single-column field at the top of the grid whose descriptive
// content lives in these fields anyway — a links row there would shift the fields beside it.
const ShortDescriptionJiraLinks = makeJiraFieldLinks("shortDescription");
const ReferenceJiraLinks = makeJiraFieldLinks("reference");
const DescriptionJiraLinks = makeJiraFieldLinks("description");

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
 * The columns are `TaskListPage.createColumns` — ten, in its order, with the three whose subject may
 * not exist gated on the backend's answer (see TaskPagesRest.addVariablesForListPage). Three of them
 * show a value that is not on `TaskDO` and is computed per row from the in-memory tree, so there is
 * nothing to sort them by (`sortable: false`), which is what Wicket says too by passing them no sort
 * property.
 *
 * Note the *tree* additionally hides `reference` and `priority` when no task in the whole tree fills
 * them. Wicket's list shows both unconditionally, and this follows the list — the divergence is
 * between the two Wicket pages, not a decision taken here.
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
  // „List view", as the tree page is headed „Tree view": the two perspectives name themselves the
  // same way they name each other (see TaskPerspectiveLink).
  titleKey: "task.list.perspective",
  columns: [
    // The one column a reader looks for first, so it stays in view while the rest scrolls sideways.
    // Headed "Structure element" rather than the field's own "Title", as both Wicket pages head it
    // (`getString("task")`, and the tree's column def) — in a list of tasks the column *is* the task.
    {
      name: "title",
      headerLabelKey: "task._",
      size: 400,
      className: "font-semibold",
      pinned: "left",
      // The whole path to the root on hover, as the Wicket list shows it in an info tooltip
      // (`WicketTaskFormatter.appendFormattedTask`, `showPathAsTooltip`).
      tooltip: (row) => row.path ?? undefined,
    },
    {
      // Painted rather than written out: the bar, its colour and its tooltip are all the backend's
      // (see Consumption.kt) — the same value and the same cell as in the tree.
      id: "consumption",
      labelKey: "task.consumption",
      accessor: (row) => row.consumption,
      cell: ({ row }) => <TaskConsumptionCell row={row.original} />,
      size: 130,
      sortable: false,
      filterKind: null,
    },
    {
      // The shared prefix of the task's cost units, with all of them as the tooltip — one column for
      // what would otherwise be a list per row (`KostHelper.getWildCardString`).
      id: "kost2WildCard",
      labelKey: "fibu.kost2",
      accessor: (row) => row.kost2WildCard ?? "",
      tooltip: (row) => row.kost2ListAsLines ?? undefined,
      size: 100,
      sortable: false,
      filterKind: null,
      visible: ({ variables }) => variables?.kost2Configured === true,
    },
    {
      // One link per order, which is why the row carries the list rather than a joined string.
      id: "orderList",
      labelKey: "fibu.auftrag.auftraege",
      accessor: (row) => row.orderList,
      cell: ({ row }) => <TaskOrdersCell row={row.original} />,
      size: 120,
      sortable: false,
      filterKind: null,
      visible: ({ variables }) => variables?.orders === true,
    },
    // The two free-text columns can carry JIRA issue keys; linked as Wicket links its description column
    // (`JiraUtils.linkJiraIssues`). The title stays the plain structure-element name, as it does in the
    // tree (see TaskTreeTable / TreeCell).
    {
      name: "shortDescription",
      size: 300,
      cell: ({ row }) => (
        <JiraLinkedText text={row.original.shortDescription} />
      ),
    },
    {
      name: "protectTimesheetsUntil",
      headerLabelKey: "task.protectTimesheetsUntil.short",
      size: 110,
      visible: ({ variables }) => variables?.protectTimesheetsUntil === true,
    },
    {
      name: "reference",
      size: 120,
      cell: ({ row }) => <JiraLinkedText text={row.original.reference} />,
    },
    { name: "priority", size: 110 },
    {
      // Coloured by the raw enum letter, worded by the bundle — as the Wicket page shows it.
      name: "status",
      size: 110,
      cell: ({ row }) => <TaskStatusListCell row={row.original} />,
    },
    {
      // Sorted by the user's surname, since the name the cell shows is assembled (`PFUserDO.displayName`
      // is transient) and Wicket's own sort — by `responsibleUserId` — orders by nothing a reader sees.
      id: "responsibleUser.lastname",
      // The entity's own wording, so the column reads as the form's field does — a computed column has
      // to name a key, and the generated metadata is where that key lives (see labelKeyFor).
      labelKey: TASK_METADATA.fields.responsibleUser.i18nKey,
      accessor: (row) => row.responsibleUser?.displayName ?? "",
      size: 160,
    },
  ],
  // The switch to the structure tree, Wicket's "tree view" button.
  listActions: TaskListActions,
  edit: {
    schema: taskSchema,
    fieldNames: TASK_FIELDS,
    defaultValues: emptyTaskValues,
    toFormValues,
    title: (task) => task.title ?? "",
    newTitleKey: "task.title.add",
    savedMessageKey: "message.successfullChanged",
    // The tree first, so it stays where an add-url without `returnTo` leads — a task is reached from
    // the tree by default, and the list always sends one.
    returnTargets: [
      // With the id of what was saved, so the tree marks that row and opens its ancestors on arrival —
      // Wicket's `PARAMETER_HIGHLIGHTED_ROW`, without which a newly added element is somewhere in a tree
      // of thousands (see the tree page).
      {
        route: TASK_TREE_ROUTE,
        labelKey: "menu.taskTree",
        highlightIdParam: HIGHLIGHT_ID_PARAM,
      },
      { route: TASK_ROUTE, labelKey: "task.list.perspective" },
      // The wizard's "create structure element" link, which expects the user back with the new element
      // — hence the id in the url it returns to (see WizardTaskStep).
      {
        route: TASK_WIZARD_ROUTE,
        labelKey: "task.wizard.pageTitle",
        highlightIdParam: HIGHLIGHT_ID_PARAM,
      },
    ],
    // "Add a subtask" from the tree: the parent is a parameter of the preset, because only the backend
    // can resolve what hangs on it — the project of the cost unit block, and the rights of the two
    // access-gated groups (see TaskPagesRest.newBaseDO and useNewEntryParams).
    newEntryParams: TASK_NEW_ENTRY_PARAMS,
    // The top menu of the Wicket form (`TaskEditPage.addTopMenuPanel`), in its order and with its
    // wording. Only the first target lives in this app; the other four are Wicket pages, and they
    // turn into routes of this app with nothing but a changed href once they are migrated.
    crossLinks: [
      // The two that are worth a button of their own beside the heading (see CrossLinkDef.prominent):
      // structuring the tree and looking at what was booked on the element are what an open task is left
      // for, and having to open a menu for them is a click on every one of them.
      {
        labelKey: "task.menu.addSubTask",
        href: (task) => `${TASK_ROUTE}/new?parentTaskId=${task.id}`,
        prominent: true,
      },
      {
        labelKey: "task.menu.addTimesheet",
        href: (task) => `wa/timesheetEdit?taskId=${task.id}`,
      },
      {
        labelKey: "task.menu.showTimesheets",
        href: (task) => `wa/timesheetList?taskId=${task.id}`,
        prominent: true,
      },
      {
        // `gantt` is the basename the Gantt pages are mounted under, so the edit page is `ganttEdit`
        // (see WebRegistry.addMountPages) — and its parameter is `task`, not `taskId`.
        labelKey: "gantt.title.add",
        href: (task) => `wa/ganttEdit?task=${task.id}`,
      },
      {
        labelKey: "task.menu.showAccessRights",
        href: (task) => `wa/accessList?taskId=${task.id}`,
      },
      // Not in Wicket's top menu, where the wizard is reachable from the tree page alone: an admin who
      // is *in* an element is exactly who wants its rights set up, and having to go back to the tree
      // and find the row again is the detour. Preset with this element, so the wizard opens on its
      // first step done (see taskWizardHref) — and offered to admins only, like the button on the two
      // task headers (TaskWizardLink), because that is who the wizard's endpoints answer.
      {
        labelKey: "task.wizard.pageTitle",
        href: (task) => taskWizardHref({ taskId: task.id }),
        adminOnly: true,
      },
    ],
    sections: [
      {
        id: "general",
        titleKey: "task.title.heading",
        fields: [
          { name: "title", emphasized: true },
          { name: "status" },
          { name: "parentTask" },
          { name: "responsibleUser" },
          { name: "priority" },
          {
            name: "maxHours",
            maxDigits: 4,
            hintKey: "task.edit.maxHoursIngoredDueToAssignedOrders",
          },
          { name: "shortDescription", span: 3 },
          { custom: ShortDescriptionJiraLinks, span: 3 },
          { name: "reference", span: 3 },
          { custom: ReferenceJiraLinks, span: 3 },
          // Last, as it is in Wicket — where it is a panel of its own, which here would be a card
          // holding nothing but one textarea.
          { name: "description", span: 3, rows: 6 },
          { custom: DescriptionJiraLinks, span: 3 },
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
