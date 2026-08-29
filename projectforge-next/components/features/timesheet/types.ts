// Mirrors org.projectforge.rest.dto.Timesheet (projectforge-rest). Keep field names in sync with the
// Spring DTO: it is what `saveorupdate` reads back, so a name that differs here is a value silently
// dropped on the way in.

import type { TIMESHEET_METADATA } from "@/lib/metadata/timesheet.generated";

/** The constants of TimesheetDO.TimeSavedByAIUnit, taken from the generated metadata. */
export type TimeSavedByAIUnit =
  (typeof TIMESHEET_METADATA.fields.timeSavedByAIUnit.enumValues)[number]["value"];

/**
 * An entity reference as the form carries it. A type alias, not an interface: the schema's `entityField`
 * infers a `looseObject` with an implicit index signature (`{ [x: string]: unknown; id: number;
 * displayName?: string }`), and TypeScript gives an alias that same implicit index signature but never an
 * interface — so only the alias is assignable to the schema's inferred type (see order/types.ts).
 */
export type EntityRefDto = {
  id: number;
  displayName?: string;
  title?: string;
  /** The task's path to the root ("A -> B -> C"), shown as the tooltip of the structure element column. */
  path?: string;
};

/**
 * A cost unit as the DTO carries it (`rest/dto/Kost2`): the id every reference is written back by, and
 * the number as the backend formatted it ("5.100.01.02") — which is what a cost unit is *called* and
 * therefore its display name here. Its project and customer come along on a recent entry and are not
 * read by the form.
 */
export type Kost2Ref = EntityRefDto & {
  formattedNumber?: string | null;
  description?: string | null;
};

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
 * (JacksonConfiguration), so an empty field is absent from the JSON rather than null — toFormValues
 * normalises it.
 */
export interface TimesheetDetail {
  /** null for a sheet that has not been saved yet (Spring assigns the id). */
  id: number | null;
  task?: EntityRefDto | null;
  user?: EntityRefDto | null;
  kost2?: Kost2Ref | null;
  location?: string | null;
  reference?: string | null;
  tag?: string | null;
  /**
   * The tags to choose from, or null/empty where none is configured — transient, set by the server
   * (`TimesheetDao.getTags`), including the sheet's own tag even after it left the configuration. The
   * hand-built form has no UILayout to read the `tag` select's values from, so TagField takes them from
   * here and shows the field only when there are any (see TIMESHEET_PAGE).
   */
  tags?: string[] | null;
  description?: string | null;
  /**
   * Both ends as an instant, ISO 8601 in UTC ("2026-08-09T08:12:34.000Z") — a `java.util.Date` as
   * Jackson writes it. The user's wall clock is derived from it against their own time zone, never the
   * browser's (see DateTimeInput and lib/user-zone.ts).
   */
  startTime?: string | null;
  stopTime?: string | null;
  timeSavedByAI?: number | null;
  timeSavedByAIUnit?: TimeSavedByAIUnit | null;
  timeSavedByAIDescription?: string | null;
  /**
   * Whether this installation tracks AI time savings at all — transient, set by the server from
   * `TimesheetDao.timeSavingsByAIEnabled`. The hand-built form has no UILayout to leave the AI fields
   * out of, so the section reads this flag instead (see TIMESHEET_PAGE).
   */
  timeSavingsByAIEnabled?: boolean;
  /**
   * The configured note shown below the form, or null/absent where none is configured — transient,
   * set by the server only when {@link timeSavingsByAIEnabled} (`ConfigurationService.timesheetNoteSavingsByAI`).
   * The hand-built form has no UILayout to carry the legacy `layoutBelowActions` alert, so AiNoteFooter
   * reads the text from here (see TIMESHEET_PAGE `editFooter`).
   */
  timeSavingsByAINote?: string | null;
  /** Position in the recent list — the backend's key for an entry that has no id (see RecentTimesheets). */
  counter?: number | null;
  deleted?: boolean;
  created?: string | null;
}

/**
 * The flat list row the live `/next/timesheet` list reads — the shape the backend now ships to the
 * next client (`TimesheetPagesRest.newDTO` + `Timesheet.copyFrom4ListRow`), one field per column of
 * `timesheet.page.tsx`. Distinct from the nested `Timesheet4ListExport` the classic React list still
 * reads via the kept UILayout.
 */
export interface TimesheetListRow {
  id: number;
  task?: EntityRefDto | null;
  user?: EntityRefDto | null;
  kost2?: Kost2Ref | null;
  startTime?: string | null;
  stopTime?: string | null;
  location?: string | null;
  reference?: string | null;
  description?: string | null;
  created?: string | null;
}
