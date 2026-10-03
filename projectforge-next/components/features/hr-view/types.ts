/**
 * The HR view ("Personalplanung", `org.projectforge.rest.hr.HRViewRest`, DTOs in `rest/dto/HRView.kt`): the
 * planned and booked man days of the employees in a period, per project or customer.
 */

/** The options of the view; every one left out is taken from the last visit by the backend. */
export interface HrViewQuery {
  /** `yyyy-MM-dd`. */
  startDay?: string;
  stopDay?: string;
  showPlanning?: boolean;
  showBookedTimesheets?: boolean;
  onlyMyProjects?: boolean;
  allProjectsGroupedByCustomer?: boolean;
  otherProjectsGroupedByCustomer?: boolean;
}

export type HrViewFilter = Required<HrViewQuery>;

export interface HrViewColumn {
  /** `p<project id>` or `k<customer number>`, the key of a row's `cells`. */
  key: string;
  label?: string | null;
  /** The task of a project; its booked days lead to the time sheets of the task. */
  taskId?: number | null;
}

/** Days, zero ones absent. */
export interface HrViewCell {
  planned?: number | null;
  actual?: number | null;
}

export interface HrViewRow {
  userId: number;
  userName?: string | null;
  planningId?: number | null;
  deleted: boolean;
  sum: HrViewCell;
  rest: HrViewCell;
  cells: Record<string, HrViewCell>;
}

export interface HrViewUser {
  id: number;
  displayName?: string | null;
}

export interface HrView {
  filter: HrViewFilter;
  /** E.g. "KW 09-12". */
  calendarWeeks: string;
  columns: HrViewColumn[];
  rows: HrViewRow[];
  unplannedUsers: HrViewUser[];
  /** Whether the user may write plannings: only then the employees lead to their planned week. */
  fullAccess: boolean;
}
