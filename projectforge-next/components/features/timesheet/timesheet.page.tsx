import { TIMESHEET_METADATA } from "@/lib/metadata/timesheet.generated";
import { definePage } from "@/lib/page-def/define-page";
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
 * The list is declared but not routed yet — nothing links to a next one and the menu entry still points
 * at Wicket (`MenuItemDefId.TIMESHEET_LIST`), so `columns` names the entity's own fields in the one shape
 * every page has and adding the list later is a route and not a restructuring (see TimesheetListRow).
 *
 * The edit page is the one that is live: the calendar opens it (see toTimesheetRoute). Its fields follow
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
  // Project management > Time sheets (MenuItemDefId.TIMESHEET_LIST under projectManagementMenu).
  categoryKey: "menu.projectmanagement",
  titleKey: "menu.timesheetList",
  // Minimal, since no list renders yet: the fields that identify a sheet, in the order the legacy list
  // shows them (`TimesheetPagesRest.createListLayout`).
  columns: [
    { name: "user", size: 140 },
    { name: "task", size: 240, className: "font-medium" },
    { name: "startTime", size: 150 },
    { name: "stopTime", size: 150 },
    { name: "location", size: 140 },
    { name: "reference", size: 140 },
    { name: "description", size: 320, wrap: true },
  ],
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
