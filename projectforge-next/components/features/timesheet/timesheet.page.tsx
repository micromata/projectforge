import { TIMESHEET_METADATA } from "@/lib/metadata/timesheet.generated";
import { definePage } from "@/lib/page-def/define-page";
import { TimesheetListActions } from "./timesheet-list-actions";
import { TimesheetStatisticsLine } from "./timesheet-statistics-line";
import type { TimesheetStatistics } from "./timesheet-statistics";
import { AiNoteFooter } from "./edit/sections/ai-note-footer";
import { TaskKost2Section } from "./edit/sections/task-kost2-section";
import { DayRangeSection } from "./edit/sections/day-range-section";
import { LocationField } from "./edit/sections/location-field";
import { ReferenceField } from "./edit/sections/reference-field";
import { TagField } from "./edit/sections/tag-field";
import { TemplatesRecentBar } from "./edit/sections/templates-recent-bar";
import {
  timesheetEditSchema,
  TIMESHEET_EDIT_FIELDS,
  type TimesheetEditValues,
} from "./edit/timesheet-edit-schema";
import {
  emptyTimesheetValues,
  toFormValues,
} from "./edit/timesheet-edit-values";
import type { EntityRefDto, TimesheetDetail, TimesheetListRow } from "./types";

/** REST category of a time sheet — the entity name every shared hook is parameterised with. */
export const TIMESHEET_ENTITY = "timesheet";
/** React Query key of the list, so a write from the edit page refreshes it once the list is built. */
export const TIMESHEET_LIST_QUERY_KEY = ["timesheet"] as const;
/** Where the calendar's slot-select preset reads from (see TimesheetPagesRest.newBaseDTO). */
const NEW_ENTRY_PARAMS = [
  "startDate",
  "endDate",
  "userId",
  "firstHour",
] as const;

/**
 * The whole time sheet page as data (see lib/page-def/types.ts).
 *
 * The list is live and routed at `next/timesheet` (`MenuItemDefId.TIMESHEET_LIST` resolves there via
 * `listUrl`): the filter toggles recursive/onlyBillable, the summed-duration + AI-share footer
 * (`statistics`), the Excel/PDF/ics exports (`listActions`) and the mass update (`massUpdate`) match the
 * legacy list — only its Vorlagen (templates) button stays behind.
 *
 * The edit page the calendar opens (see toTimesheetRoute). Its fields follow
 * the legacy form (`TimesheetPagesRest.createEditLayout`) — the task and its cost unit, the period, the
 * texts — with the templates/recent bar above them and the AI-time-savings block only where the
 * installation tracks it (`timeSavingsByAIEnabled`).
 */
export const TIMESHEET_PAGE = definePage<
  TimesheetListRow,
  TimesheetEditValues,
  TimesheetDetail,
  typeof TIMESHEET_METADATA
>({
  entity: TIMESHEET_ENTITY,
  metadata: TIMESHEET_METADATA,
  route: "/timesheet",
  queryKey: TIMESHEET_LIST_QUERY_KEY,
  // Served one page at a time (POST listPage): the list sorts only on DB columns and its onlyBillable option
  // is a CustomResultFilter that runs inside the query pipeline, so nothing narrows or re-sorts after it — the
  // page slice is a faithful window on the whole result. The summed-duration + AI-share footer comes from the
  // aggregate hook over the full id list (see TimesheetPagesRest.aggregate, PageDef.serverPaging).
  serverPaging: true,
  // Project management > Time sheets (MenuItemDefId.TIMESHEET_LIST under projectManagementMenu).
  categoryKey: "menu.projectmanagement",
  titleKey: "menu.timesheetList",
  // The fields that identify a sheet, in the order the legacy list shows them
  // (`TimesheetPagesRest.createListLayout`).
  columns: [
    // Both are entity references the row carries as `{ id, displayName }`, not a plain value, so each
    // names the string it shows rather than letting the default cell stringify the object. The sort id
    // stays the field name, which is what the backend orders the server-side pages by.
    {
      name: "user",
      size: 140,
      cell: ({ row }) => row.original.user?.displayName ?? null,
    },
    {
      name: "task",
      size: 240,
      // The plain task title (the backend drops the "(#id)" of its display name), with the path to the
      // root as the tooltip — the Wicket column shows exactly this (`TaskPropertyColumn`).
      cell: ({ row }) => (
        <span className="font-medium">
          {row.original.task?.title ?? row.original.task?.displayName ?? null}
        </span>
      ),
      tooltip: (row) => row.task?.path ?? undefined,
    },
    { name: "startTime", size: 150 },
    { name: "stopTime", size: 150 },
    { name: "location", size: 140 },
    { name: "reference", size: 140 },
    { name: "description", size: 320, wrap: true },
  ],
  // The list's footer between the toolbar and the table: the summed duration and, where the installation
  // tracks it, the AI share — the two numbers the legacy list shows (TimesheetPagesRest.postProcessResultSet).
  // The cast is where the untyped `ResultSet.statistics` becomes what the rest class sends (see
  // PageDef.statistics for why this is the place for it).
  statistics: ({ statistics, isFetching }) => (
    <TimesheetStatisticsLine
      statistics={statistics as TimesheetStatistics | undefined}
      isFetching={isFetching}
    />
  ),
  // The list's exports, in the toolbar: the filtered sheets as Excel or PDF, and the ics subscription url
  // (see TimesheetListActions). The PDF is built with OpenPDF in the backend now, no longer the wicket FOP.
  listActions: TimesheetListActions,
  // "Mehrfachauswahl" — the legacy list's mass select and update, backed by TimesheetMultiSelectedPageRest
  // (mounted under `timesheetSelected`, the entity's own name + URL_SUFFIX_SELECTED, not `${entity}Selected`).
  // The selection column and mode toggle appear only for a user with update access; the mass-update form
  // itself is the backend's UILayout, rendered by the generic MassUpdatePage under the route below.
  massUpdate: {
    endpoint: "timesheetSelected",
    route: "/timesheet/mass-update",
  },
  edit: {
    schema: timesheetEditSchema,
    fieldNames: TIMESHEET_EDIT_FIELDS,
    defaultValues: emptyTimesheetValues,
    toFormValues,
    // The task the sheet is booked on, and the live one once another is picked — so the heading follows
    // the select. Its `title` is the task's plain name, without the "(#id)" the backend appends to
    // `displayName` to keep it unique in a flat list; the picker already stores that plain name (see
    // TaskSelectField), so the fallback covers a freshly picked task too.
    title: (timesheet, values) => {
      const task = (values.task ?? timesheet.task) as EntityRefDto | null;
      return task?.title ?? task?.displayName ?? "";
    },
    newTitleKey: "timesheet.title.add",
    savedMessageKey: "message.successfullChanged",
    newEntryParams: NEW_ENTRY_PARAMS,
    // Offer the clone, as Wicket does (TimesheetPagesRest.cloneSupport). The button builds a new sheet
    // from the one on screen — the backend's `cloneData` prepares it (id and timestamps dropped, the
    // default prepareClone; AUTOSAVE is not honoured there, only NONE turns clone off, see
    // AbstractEntityRest.cloneData), and the add form opens under `/timesheet/new?clone=1`.
    clone: true,
    // "In Termin umwandeln" — build a calendar event from this sheet's span and texts and open it as a
    // new event (TimesheetPagesRest.switch2CalendarEvent → TeamEventPagesRest.cloneFromTimesheet). The
    // team event is named, not imported, so the two features don't depend on each other in a circle.
    convert: {
      action: "switch2CalendarEvent",
      targetEntity: "teamEvent",
      targetRoute: "/teamEvent",
      labelKey: "plugins.teamcal.switchToTeamEventButton",
    },
    // Save and cancel come back to the calendar, which is the only thing that opens the form — there is
    // no timesheet list of this app to return to (see toTimesheetRoute).
    returnTargets: [{ route: "/calendar", labelKey: "menu.calendar" }],
    // The templates/recent bar sits above the sections and stays visible while the user scrolls — the
    // legacy form's `timesheet.edit.templatesAndRecent` widget, which is not a field of any section.
    editBanner: TemplatesRecentBar,
    // Below the form: the configured AI-time-savings note the legacy UILayout put in
    // `layoutBelowActions`, shown only where the installation tracks AI savings and a text is
    // configured (see AiNoteFooter, TimesheetPagesRest.timeSavingsByAINote).
    editFooter: AiNoteFooter,
    sections: [
      {
        id: "general",
        titleKey: "timesheet",
        fields: [
          // Task, its cost unit and the task's consumption in one block — the task decides the other two
          // (see TaskKost2Section). A full row, so its own three columns line up with the grid's.
          { custom: TaskKost2Section, span: 3 },
          // User, start, stop and the duration between them — who and when on one line (see
          // DayRangeSection, which declares the user field for that reason).
          { custom: DayRangeSection, span: 3 },
          { custom: LocationField },
          // A select of the configured tags, rendered only where any are configured (see TagField).
          { custom: TagField },
          { custom: ReferenceField },
          { name: "description", rows: 5, span: 3 },
        ],
      },
      {
        id: "ai",
        // "Time savings AI" — the heading the legacy form gave the block (`timesheet.ai.timeSavedByAI`).
        titleKey: "timesheet.ai.timeSavedByAI",
        // Only where the installation tracks AI time savings, the backend's answer on the loaded (and on
        // a new) entry — the form has no UILayout to leave the fields out of (see TimesheetDetail).
        visible: ({ data }) => data?.timeSavingsByAIEnabled === true,
        fields: [
          { name: "timeSavedByAI" },
          { name: "timeSavedByAIUnit" },
          { name: "timeSavedByAIDescription" },
        ],
      },
    ],
  },
});
