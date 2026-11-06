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

import type { PushTokenRequest } from '@projectforge/shared/rs/app-auth';

import { ApiClient } from '../api/apiClient';
import { appLog } from '../log/appLog';

export interface PushSubscription {
  remove(): void;
}

/** A push as the app sees it: the id of its request and its link (empty if it has none). */
export interface PushTap {
  id: string;
  link: string;
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
  onReceived(listener: () => void): PushSubscription;
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
 * @param production Fallback if the signing is unknown: debug builds are signed for the sandbox of APNs, release builds
 * (TestFlight, store) for production. A token is valid for one of them only.
 */
export class PushService {
  private sentToken: string | null = null;
  private lastTap: string | null = null;
  private subscriptions: PushSubscription[] = [];

  constructor(
    private readonly api: ApiClient,
    private readonly platform: PushPlatform,
    private readonly options: {
      production: boolean;
      /** A push was tapped: its link (empty if it has none). */
      onLink: (link: string) => void;
      /** A push arrived while the app is open. */
      onReceived: () => void;
    },
  ) {}

  /** Asks for the permission, registers at APNs and hands the token to the server. The tap that started the app is passed on. */
  async start() {
    this.stop();
    this.subscriptions = [
      this.platform.onTap(this.tapped),
      this.platform.onReceived(() => this.options.onReceived()),
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
    this.options.onLink(tap.link);
  };

  private async register(token: string) {
    if (token === this.sentToken) return;
    const body: PushTokenRequest = {
      token,
      environment: this.platform.environment() ?? (this.options.production ? 'PRODUCTION' : 'SANDBOX'),
    };
    try {
      await this.api.post('/rs/appAuth/pushToken', { body, renewLogin: false });
      this.sentToken = token;
      appLog.debug('push', `Push token sent (${body.environment})`);
    } catch (e) {
      // Offline or refused: tried again with the next start of the app.
      appLog.debug('push', `Push token not sent: ${String(e)}`);
    }
  }
}
