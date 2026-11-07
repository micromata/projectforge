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

import * as Notifications from 'expo-notifications';

import PfNative from '../../../modules/pf-native/src/PfNativeModule';
import { PushPlatform, PushTap } from './pushService';

// A push while the app is open: shown as a banner (the app loads the notifications, see PushService).
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

/**
 * The link, the device and the server of the server's push are beside `aps` in its payload (ApnsPushSender);
 * expo-notifications hands them over in the payload of the trigger only (its `data` is the payload's `body`).
 */
function payloadOf(request: Notifications.NotificationRequest) {
  const trigger = request.trigger as { type?: string; payload?: Record<string, unknown> } | null;
  const value = (key: string) => trigger?.payload?.[key] ?? request.content.data?.[key];
  const link = value('link');
  const device = value('device');
  const server = value('server');
  return {
    link: typeof link === 'string' ? link : '',
    device: typeof device === 'number' ? device : null,
    server: typeof server === 'string' ? server : null,
  };
}

function tapOf(response: Notifications.NotificationResponse): PushTap {
  const { request } = response.notification;
  return { id: request.identifier, ...payloadOf(request) };
}

/** The pushes of iOS by expo-notifications. */
export const expoPush: PushPlatform = {
  requestPermission: async () => {
    const answer = await Notifications.requestPermissionsAsync({
      ios: { allowAlert: true, allowBadge: true, allowSound: true },
    });
    return answer.granted;
  },
  deviceToken: async () => (await Notifications.getDevicePushTokenAsync()).data as string,
  onToken: (listener) => Notifications.addPushTokenListener((token) => listener(token.data as string)),
  onTap: (listener) => Notifications.addNotificationResponseReceivedListener((response) => listener(tapOf(response))),
  takeLastTap: async () => {
    const response = await Notifications.getLastNotificationResponseAsync();
    if (response == null) return null;
    await Notifications.clearLastNotificationResponseAsync();
    return tapOf(response);
  },
  onReceived: (listener) =>
    Notifications.addNotificationReceivedListener((notification) => {
      const { request } = notification;
      const { device, server } = payloadOf(request);
      const badge = request.content.badge;
      listener({ badge: typeof badge === 'number' ? badge : null, device, server });
    }),
  setBadge: (count) => Notifications.setBadgeCountAsync(count),
  environment: () => PfNative.apsEnvironment(),
};
