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

import { Tabs, useRouter } from 'expo-router';
import { useEffect, useRef } from 'react';
import { Alert, Platform } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { authController, useAuth } from '../../core/auth/authController';
import { compatibilityIssue } from '../../core/compat/compatibility';
import { appVersion } from '../../core/platform';
import { expoPush } from '../../core/push/expoPush';
import { PushService } from '../../core/push/pushService';
import { chatEnabled, userStatus, useUserStatus } from '../../core/status/userStatus';
import { isChatPath, webPathOfLink } from '../../core/web/links';
import { useColors } from '../../core/ui/colors';
import { compatibilityText, TabBar } from '../../features/home/TabBar';
import { webTabs } from '../../features/web/webTabs';

/**
 * The tabs of the logged-in app. A new login (another server, another user) starts them anew: keyed by the
 * generation of the login, the WebViews begin with a new web session and nothing of the former one.
 */
export default function TabsLayout() {
  const auth = useAuth();
  const colors = useColors();
  const { status } = useUserStatus();
  const issue = compatibilityIssue({
    appVersion,
    serverVersion: status?.systemData?.version,
    minAppVersion: status?.systemData?.minAppVersion,
  });
  const shownIssue = useRef<string | null>(null);
  const router = useRouter();

  useEffect(() => webTabs.reset(), [auth.generation]);

  // Asks for the permission to show pushes, registers and hands the token to the server; a tapped push opens its
  // link in the chat tab (a page of the chat) or in the pages.
  useEffect(() => {
    const api = authController.api;
    if (Platform.OS !== 'ios' || api == null) return;
    const service = new PushService(api, expoPush, {
      production: !__DEV__,
      onLink: (link) => {
        const path = webPathOfLink(link);
        if (path == null) return;
        const chat = isChatPath(path) && chatEnabled(userStatus.get().status);
        webTabs.open(chat ? 'chat' : 'pages', path);
        router.dismissTo(chat ? '/chat' : '/pages');
      },
      onReceived: () => void userStatus.refresh(),
    });
    void service.start().catch(() => {});
    return () => service.stop();
  }, [auth.generation, router]);

  // The number on the icon follows the notifications.
  const openNotifications = status?.notifications?.openCount;
  useEffect(() => {
    if (Platform.OS === 'ios' && openNotifications != null) expoPush.setBadge(openNotifications).catch(() => {});
  }, [openNotifications]);

  // The compatibility warning once per server and app start, not on every poll.
  useEffect(() => {
    if (issue == null) return;
    const key = `${auth.session?.serverUrl}#${issue.kind}#${issue.minVersion}`;
    if (shownIssue.current === key) return;
    shownIssue.current = key;
    Alert.alert(compatibilityText(issue));
  }, [issue, auth.session?.serverUrl]);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.background }} edges={['top', 'left', 'right']}>
      <Tabs
        key={auth.generation}
        screenOptions={{ headerShown: false, sceneStyle: { backgroundColor: colors.background } }}
        tabBar={(props) => <TabBar {...props} issue={issue} />}
      >
        <Tabs.Screen name="cards" />
        <Tabs.Screen name="addresses" />
        <Tabs.Screen name="timesheets" />
        <Tabs.Screen name="pages" />
        <Tabs.Screen name="chat" />
      </Tabs>
    </SafeAreaView>
  );
}
