/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

// The time sheets of the user (TimesheetEntityRest, TimesheetFavoritesRest, /rs/timesheet) and the tasks to book on
// (TaskServicesRest, /rs/task). See src/rs/app-auth.ts for @server.

/** The bookings of a period and the tags offered. @server org.projectforge.rest.TimesheetEntityRest$AppTimesheetList */
export interface AppTimesheetList {
  timesheets: TimesheetData[];
  /** The tags the installation offers, empty where none is configured. */
  tags?: string[] | null;
}

/** @server org.projectforge.rest.TimesheetEntityRest$RecentTimesheets */
export interface RecentTimesheets {
  timesheets: TimesheetData[];
}

/**
 * A booking. Its whole json is loaded and sent back by an edit, so fields this app doesn't show (AI time savings, …)
 * survive.
 * @server org.projectforge.rest.dto.Timesheet
 */
export interface TimesheetData {
  id?: number | null;
  task?: TimesheetTask | null;
  kost2?: TimesheetKost2 | null;
  /** ISO date-time (UTC). */
  startTime?: string | null;
  stopTime?: string | null;
  description?: string | null;
  location?: string | null;
  reference?: string | null;
  tag?: string | null;
  /** Booked by a proposed grant and not yet accepted by the project management. */
  acceptance?: unknown;
}

/** The task of a booking. @server org.projectforge.rest.dto.Task */
export interface TimesheetTask {
  id: number;
  title?: string | null;
  /** The ancestors as one text, `A -> B`. */
  path?: string | null;
}

/** The cost unit of a booking. @server org.projectforge.rest.dto.Kost2 */
export interface TimesheetKost2 {
  id: number;
  formattedNumber?: string | null;
  description?: string | null;
  /** `5.123.45.01: <Kost2Art> - <project>`, the only text the rows of `appList` have. */
  displayName?: string | null;
}

/** What a new or changed booking sends (inside `{ data }`). @request org.projectforge.rest.dto.Timesheet */
export interface TimesheetSave {
  id?: number | null;
  task: { id: number };
  kost2: { id: number } | null;
  startTime: string;
  stopTime: string;
  description: string;
  location: string;
  reference: string;
  tag: string | null;
  user?: { id: number };
}

/** The body of insert, update, markAsDeleted and undelete. @request org.projectforge.rest.dto.PostData */
export interface PostData<T> {
  data: T;
}

/** A template of the user. @server org.projectforge.business.timesheet.TimesheetFavorite */
export interface TimesheetFavorite {
  id: number;
  name?: string | null;
  taskId?: number | null;
  cost2Id?: number | null;
  description?: string | null;
  location?: string | null;
  reference?: string | null;
  tag?: string | null;
}

/** @request org.projectforge.rest.TimesheetFavoritesRest$NewTimesheetFavorite */
export interface NewTimesheetFavorite {
  name: string;
  timesheet: Omit<TimesheetSave, 'startTime' | 'stopTime' | 'user' | 'id'>;
}

/** A hit of the task search, the whole path as `A | B | C`. @server org.projectforge.rest.core.AbstractEntityRest$DisplayObject */
export interface DisplayObject {
  id: number;
  displayName?: string | null;
}

/** A node of one level of the task tree. @server org.projectforge.rest.task.TaskServicesRest$TaskChild */
export interface TaskChild {
  id: number;
  title?: string | null;
  /** There is a level below to open. */
  hasChildren: boolean;
  /** A time sheet may be booked on it. */
  bookable: boolean;
}

/** A task with the cost units a booking on it may charge (`info/{id}`). @server org.projectforge.rest.task.TaskServicesRest$Task */
export interface TaskInfo {
  id: number;
  title?: string | null;
  /** The ancestors from the top. */
  path?: TaskInfo[] | null;
  kost2List?: TaskKost2[] | null;
}

/** @server org.projectforge.rest.task.TaskServicesRest$Kost2 */
export interface TaskKost2 {
  id: number;
  /** `5.123.45.01: <Kost2Art> - <project>` */
  title: string;
}
