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

import type { PushTokenRequest, PushTokenResponse } from '@projectforge/shared/rs/app-auth';

import { ApiClient } from '../api/apiClient';
import { Session } from '../auth/session';
import { appLog } from '../log/appLog';
import { parseUrl } from '../url';

export interface PushSubscription {
  remove(): void;
}

/**
 * A push as the app sees it: the id of its request, its link (empty if it has none) and the app device it was sent to
 * (`Session.deviceId`, which tells the login; null from an older server).
 */
export interface PushTap {
  id: string;
  link: string;
  device: number | null;
  /** The host of the server (its configured domain), null from an older server. */
  server: string | null;
}

/** A push arrived while the app is open: the number on the icon it carries (the server's open count), null without. */
export interface PushReceived {
  badge: number | null;
  device: number | null;
  server: string | null;
}

/**
 * The stored login a push belongs to if it isn't the active one, null for the active one (also for a push without
 * device, of an older server, which pushes only while it is shown). The ids of the devices are counted per server: of
 * two logins with the same id, the one at the push's host.
 */
export function pushTarget(
  push: { device: number | null; server: string | null },
  sessions: Session[],
  active: Session | null,
): Session | null {
  if (push.device == null) return null;
  let candidates = sessions.filter((it) => it.deviceId === push.device);
  if (candidates.length > 1 && push.server) {
    candidates = candidates.filter((it) => parseUrl(it.serverUrl)?.hostname === push.server);
  }
  if (candidates.length !== 1) return null;
  const target = candidates[0];
  return active && target.serverUrl === active.serverUrl && target.userId === active.userId ? null : target;
}

/** What the push service needs of the system (expoPush.ts), kept apart so it can be tested without it. */
export interface PushPlatform {
  /** Asks for the permission to show pushes (the system asks once); true if they may be shown. */
  requestPermission(): Promise<boolean>;
  /** Registers at APNs; the device token (hex). */
  deviceToken(): Promise<string>;
  /** A new device token (APNs may change it while the app runs). */
  onToken(listener: (token: string) => void): PushSubscription;
  /** A push was tapped while the app runs. */
  onTap(listener: (tap: PushTap) => void): PushSubscription;
  /** The tap of a push that started the app (or the last one), null if there is none; forgotten once read. */
  takeLastTap(): Promise<PushTap | null>;
  /** A push arrived while the app is open. */
  onReceived(listener: (push: PushReceived) => void): PushSubscription;
  /** The number on the icon of the app. */
  setBadge(count: number): Promise<unknown>;
  /** The APNs environment of the signing, null if unknown. */
  environment(): PushTokenRequest['environment'] | null;
}

/**
 * Pushes (APNs) of the server wake the app for a notification. iOS only so far. The push carries no text of the
 * notification, only a generic one, its link and the count: the app loads the content itself. Port of the Flutter
 * app's PushService.
 *
 * The other stored logins ([others]) get the token too: their servers push as well, naming the device, so a push is
 * told apart by its login. A server that doesn't name it (older, `deviceTagged` missing) gets it removed again: its push
 * would look like one of the server shown.
 *
 * @param production Fallback if the signing is unknown: debug builds are signed for the sandbox of APNs, release builds
 * (TestFlight, store) for production. A token is valid for one of them only.
 */
export class PushService {
  /** The token sent (or being sent): the token of the registration comes as an event too. */
  private sentToken: string | null = null;
  private lastTap: string | null = null;
  private subscriptions: PushSubscription[] = [];

  constructor(
    private readonly api: ApiClient,
    private readonly platform: PushPlatform,
    private readonly options: {
      production: boolean;
      /** A push was tapped. */
      onTap: (tap: PushTap) => void;
      /** A push arrived while the app is open. */
      onReceived: (push: PushReceived) => void;
      /** The clients of the stored logins other than the active one. */
      others?: () => Promise<ApiClient[]>;
    },
  ) {}

  /** Asks for the permission, registers at APNs and hands the token to the server. The tap that started the app is passed on. */
  async start() {
    this.stop();
    this.subscriptions = [
      this.platform.onTap(this.tapped),
      this.platform.onReceived((push) => {
        appLog.debug('push', `Push received (badge ${push.badge ?? '-'}, device ${push.device ?? '-'})`);
        this.options.onReceived(push);
      }),
      this.platform.onToken((token) => void this.register(token)),
    ];
    const launch = await this.platform.takeLastTap();
    if (launch) this.tapped(launch);
    if (!(await this.platform.requestPermission())) {
      appLog.info('push', 'Pushes not allowed');
      return;
    }
    await this.register(await this.platform.deviceToken());
  }

  stop() {
    this.subscriptions.forEach((it) => it.remove());
    this.subscriptions = [];
  }

  /** The tap that started the app may come as an event too: followed once. */
  private tapped = (tap: PushTap) => {
    if (tap.id === this.lastTap) return;
    this.lastTap = tap.id;
    this.options.onTap(tap);
  };

  private async register(token: string) {
    if (token === this.sentToken) return;
    const body: PushTokenRequest = {
      token,
      environment: this.platform.environment() ?? (this.options.production ? 'PRODUCTION' : 'SANDBOX'),
    };
    this.sentToken = token;
    try {
      await this.api.post('/rs/appAuth/pushToken', { body, renewLogin: false });
      appLog.debug('push', `Push token sent (${body.environment})`);
    } catch (e) {
      // Offline or refused: tried again with the next start of the app.
      if (this.sentToken === token) this.sentToken = null;
      appLog.debug('push', `Push token not sent: ${String(e)}`);
    }
    await this.registerOthers(body);
  }

  private async registerOthers(body: PushTokenRequest) {
    let clients: ApiClient[];
    try {
      clients = (await this.options.others?.()) ?? [];
    } catch (e) {
      appLog.debug('push', `Other logins not read: ${String(e)}`);
      return;
    }
    await Promise.all(
      clients.map(async (client) => {
        try {
          const answer = await client.post<PushTokenResponse | undefined>('/rs/appAuth/pushToken', {
            body,
            renewLogin: false,
          });
          if (answer?.deviceTagged === true) {
            appLog.debug('push', 'Push token sent to another server');
          } else {
            await client.post('/rs/appAuth/pushToken/remove', { renewLogin: false });
            appLog.debug('push', 'Another server pushes without the device: push token removed there');
          }
        } catch (e) {
          // Offline, logged out or refused: tried again with the next start.
          appLog.debug('push', `Push token not sent to another server: ${String(e)}`);
        }
      }),
    );
  }
}
