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

import type { AppNotification } from '@projectforge/shared/rs/notification';

import type { ApiClient } from '../../core/api/apiClient';

export type { AppNotification } from '@projectforge/shared/rs/notification';

/** A banner stays until it is done. */
export const isBanner = (it: AppNotification) => it.display === 'BANNER';

/** As in the web: "confirm" while it is open, "done" for a banner the user may end. */
export const canAcknowledge = (it: AppNotification) => it.status === 'OPEN';

export const canDone = (it: AppNotification) => isBanner(it) && it.manualDone;

/** The date the notification was created (the server writes epoch millis or an ISO date). */
export function createdAt(it: AppNotification): Date | null {
  const value = it.created;
  if (value == null) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Banners first, otherwise the order of the server (newest first). */
export function sortForDisplay(list: AppNotification[]): AppNotification[] {
  return [...list].sort((a, b) => (isBanner(b) ? 1 : 0) - (isBanner(a) ? 1 : 0));
}

/**
 * The text of a notification (sanitized html) as plain text: paragraphs and lists become lines, tags go, the common
 * entities are resolved.
 */
export function htmlToText(html?: string | null): string {
  let text = (html ?? '')
    .replace(/<\s*br\s*\/?\s*>/gi, '\n')
    .replace(/<\s*li[^>]*>/gi, '\n• ')
    .replace(/<\s*\/\s*(p|div|ul|ol|h[1-6]|tr)\s*>/gi, '\n')
    .replace(/<[^>]*>/g, '');
  const entities: [string, string][] = [
    ['&nbsp;', ' '],
    ['&lt;', '<'],
    ['&gt;', '>'],
    ['&quot;', '"'],
    ['&#39;', "'"],
    ['&amp;', '&'], // Last: "&amp;lt;" is the text "&lt;".
  ];
  for (const [entity, value] of entities) text = text.split(entity).join(value);
  return text
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

/** The notifications of the user (NotificationRest, `/rs/notification`). Port of the Flutter app's repository. */
export class NotificationsRepository {
  private static readonly BASE = '/rs/notification';

  constructor(private readonly api: Pick<ApiClient, 'get' | 'post'>) {}

  /** The visible ones, newest first. Loaded when the status changes too: a refusal never opens a login. */
  loadOpen(): Promise<AppNotification[]> {
    return this.api.get<AppNotification[]>(`${NotificationsRepository.BASE}/my`, { renewLogin: false });
  }

  /** The ones no longer visible of the last 90 days. */
  loadArchived(): Promise<AppNotification[]> {
    return this.api.get<AppNotification[]>(`${NotificationsRepository.BASE}/my/archived`);
  }

  /** Marks it as read (a banner stays until it is done). */
  async acknowledge(id: number): Promise<void> {
    await this.api.post(`${NotificationsRepository.BASE}/${id}/acknowledge`);
  }

  /** Marks it as done (only if [AppNotification.manualDone]). */
  async done(id: number): Promise<void> {
    await this.api.post(`${NotificationsRepository.BASE}/${id}/done`);
  }
}
