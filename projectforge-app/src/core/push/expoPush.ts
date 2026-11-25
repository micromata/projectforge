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
import { AndroidImportance } from 'expo-notifications';
import { Platform } from 'react-native';

import PfNative from '../../../modules/pf-native/src/PfNativeModule';
import { payloadOf } from './pushPayload';
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

function tapOf(response: Notifications.NotificationResponse): PushTap {
  const { request } = response.notification;
  const { link, device, server } = payloadOf(request);
  return { id: request.identifier, link, device, server };
}

/**
 * The channels of the Android notifications (FcmPushSender names them): since Android 8 the sound belongs to the
 * channel, not to the single notification: IMPORTANT and URGENT ones ring, the others only show.
 */
async function createChannels() {
  await Notifications.setNotificationChannelAsync('pf_default', {
    name: 'Notifications',
    importance: AndroidImportance.HIGH,
    sound: null,
  });
  await Notifications.setNotificationChannelAsync('pf_important', {
    name: 'Important notifications',
    importance: AndroidImportance.HIGH,
    sound: 'default',
  });
}

/** The pushes of iOS (APNs) and Android (FCM) by expo-notifications. */
export const expoPush: PushPlatform = {
  requestPermission: async () => {
    // Before the permission: Android 13 asks for it only once a channel exists.
    if (Platform.OS === 'android') await createChannels();
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
      const { device, server, badge: dataBadge } = payloadOf(request);
      // iOS: the badge of aps; Android has none in the notification, the server sends it as data.
      const badge = request.content.badge;
      listener({ badge: typeof badge === 'number' ? badge : dataBadge, device, server });
    }),
  setBadge: (count) => Notifications.setBadgeCountAsync(count),
  // APNs only: FCM has no sandbox.
  environment: () => (Platform.OS === 'ios' ? PfNative.apsEnvironment() : null),
};
