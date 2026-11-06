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

// The notifications of the user (NotificationRest, /rs/notification). See src/rs/app-auth.ts for @server.

/** A notification, newest first in the lists. @server org.projectforge.rest.notification.NotificationRest$Notification */
export interface AppNotification {
  id: number;
  /** `INFO`, `IMPORTANT` or `URGENT`. */
  severity: string;
  /** `TOAST`, `TOAST_CONFIRM` or `BANNER` (stays until it is done). */
  display: string;
  /** `OPEN`, `ACKNOWLEDGED`, `DONE`, `RESOLVED` or `EXPIRED`. */
  status: string;
  /** The user may mark it as done (otherwise a rule resolves it). */
  manualDone: boolean;
  title?: string | null;
  /** Sanitized html. */
  body?: string | null;
  /** A path of the next app (`/chat/5?message=7`, `/timesheet`). */
  link?: string | null;
  /** Epoch millis or an ISO date, as the server's Jackson writes a Date. */
  created?: number | string | null;
}
