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

/**
 * What the server's push carries beside its text, read from what expo-notifications hands over. Kept apart from
 * expoPush.ts (native modules), so it can be tested.
 */
export interface PushPayload {
  link: string;
  device: number | null;
  server: string | null;
  badge: number | null;
}

/** The part of expo-notifications' request the payload is read from. */
export interface PushRequestShape {
  trigger?: unknown;
  content: { data?: Record<string, unknown> | null };
}

/** The numbers of the data of an Android push are strings (FCM data is text only); iOS has them as numbers. */
export function numberOf(value: unknown): number | null {
  if (typeof value === 'number') return value;
  if (typeof value === 'string' && /^\d+$/.test(value)) return Number(value);
  return null;
}

/**
 * The link, the device and the server of the server's push are beside `aps` in its payload (ApnsPushSender);
 * expo-notifications hands them over in the payload of the trigger only (its `data` is the payload's `body`). On
 * Android they are the data of the message (FcmPushSender), text only.
 */
export function payloadOf(request: PushRequestShape): PushPayload {
  const trigger = request.trigger as { type?: string; payload?: Record<string, unknown> } | null | undefined;
  const value = (key: string) => trigger?.payload?.[key] ?? request.content.data?.[key];
  const link = value('link');
  const server = value('server');
  return {
    link: typeof link === 'string' ? link : '',
    device: numberOf(value('device')),
    server: typeof server === 'string' ? server : null,
    badge: numberOf(value('badge')),
  };
}
