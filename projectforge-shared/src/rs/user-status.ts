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

// The user status (UserStatusRest, GET /rs/userStatus) and the theme of the web pages (UISettingsRest). See
// src/rs/app-auth.ts for @server.

import type { SystemData } from './app-auth';

/**
 * What the app shows of the server outside of its pages: the badge of the chat tab, the bell of the notifications,
 * the message of the administrators, the theme and the compatibility warning.
 * @server org.projectforge.rest.UserStatusRest$Result
 */
export interface UserStatusResult {
  /** The unread messages in all chat rooms and direct messages. */
  chatUnread?: number;
  /** The features switched off on the server (`MenuItemDefId`), e.g. `CHAT`: the app shows no chat tab then. */
  disabledFeatures?: string[];
  notifications?: NotificationSummary | null;
  /** The message of the administrators (e.g. maintenance), null without one. */
  alertMessage?: string | null;
  /** The user's theme of the web pages: `light`, `dark` or `system`. */
  theme?: string | null;
  /** The version of the server and the oldest app version it works with. */
  systemData?: SystemData;
}

/** @server org.projectforge.business.notification.NotificationDao$Summary */
export interface NotificationSummary {
  /** The notifications visible to the user. */
  openCount: number;
  /** The most severe of them: `INFO`, `IMPORTANT`, `URGENT`, null without any. */
  maxSeverity?: string | null;
  /** The id of the newest one: with openCount it tells when the list has to be loaded again. */
  latestId?: number | null;
}

/** For a server whose user status has no theme yet. @server org.projectforge.rest.UIThemeSettings */
export interface UIThemeSettings {
  theme?: string | null;
}
