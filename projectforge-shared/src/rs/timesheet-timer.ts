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

// The timer of the user (TimesheetTimerRest, /rs/timesheetTimer), shared by the app and the web pages. See
// src/rs/app-auth.ts for @server.

/** The running timer, `running` false without one. @server org.projectforge.rest.TimesheetTimerRest$TimerInfo */
export interface TimesheetTimerInfo {
  running: boolean;
  /** UTC, milliseconds since 1970 (the server's clock). */
  startedAt?: number | null;
  taskId?: number | null;
  kost2Id?: number | null;
  description?: string | null;
  location?: string | null;
  reference?: string | null;
  tag?: string | null;
  /** The server's clock now, in milliseconds. */
  now?: number | null;
}

/** What a started timer books on, all optional. @request org.projectforge.rest.TimesheetTimerRest$StartRequest */
export interface TimesheetTimerStartRequest {
  taskId?: number | null;
  kost2Id?: number | null;
  description?: string | null;
  location?: string | null;
  reference?: string | null;
  tag?: string | null;
}
