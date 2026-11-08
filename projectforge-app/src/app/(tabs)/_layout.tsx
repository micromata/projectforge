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
import { useEffect, useState } from 'react';
import { Platform, StyleSheet, useWindowDimensions, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { authController, useAuth } from '../../core/auth/authController';
import { Session } from '../../core/auth/session';
import { compatibilityIssue } from '../../core/compat/compatibility';
import { appVersion } from '../../core/platform';
import { useAppSettings } from '../../core/settings/appSettings';
import { expoPush } from '../../core/push/expoPush';
import { PushService, pushTarget } from '../../core/push/pushService';
import { otherServers, useOtherServers } from '../../core/status/otherServers';
import { chatEnabled, userStatus, useUserStatus } from '../../core/status/userStatus';
import { isChatPath, webPathOfLink } from '../../core/web/links';
import { useColors } from '../../core/ui/colors';
import { tabSheets } from '../../core/ui/tabSheets';
import { CompatibilityBanner } from '../../features/home/CompatibilityBanner';
import { MoreMenu } from '../../features/home/MoreMenu';
import { askToSwitch } from '../../features/home/switchServer';
import { TabBar } from '../../features/home/TabBar';
import { ServerSwitchSheet } from '../../features/home/ServerSwitchSheet';
import { NotificationsSheet } from '../../features/notifications/NotificationsSheet';
import { SettingsSheet } from '../../features/settings/SettingsSheet';
import { timesheets } from '../../features/timesheets/timesheetsStore';
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
  // The compatibility warning stays until it is confirmed, once per server and issue (the menu keeps showing it).
  const issueKey = issue && `${auth.session?.serverUrl}#${issue.kind}#${issue.minVersion}`;
  const [confirmedIssue, setConfirmedIssue] = useState<string | null>(null);
  const router = useRouter();
  // In landscape the tabs are a rail at the side, as in the Flutter app: the height is what the pages lack there. Left
  // or right as chosen in the settings.
  const { width, height } = useWindowDimensions();
  const { railSide } = useAppSettings();
  const rail = width > height ? railSide : null;
  // The sheets of the tab bar leave the bar free.
  const [bar, setBar] = useState({ width: 0, height: 0 });

  // Logged out: no sheet is shown at the next login.
  useEffect(() => tabSheets.close, []);

  useEffect(() => webTabs.reset(auth.generation), [auth.generation]);

  // Asks for the permission to show pushes, registers and hands the token to the servers of all logins; a tapped push
  // opens its link in the chat tab (a page of the chat) or in the pages, one of another server after asking to switch.
  useEffect(() => {
    const api = authController.api;
    if (api == null) return;
    otherServers.start({
      sessions: authController.otherSessions,
      clientFor: authController.clientFor,
    });
    if (Platform.OS !== 'ios') return () => otherServers.stop();
    const openLink = (link: string, other: Session | null) => {
      const path = webPathOfLink(link);
      const chat = path != null && isChatPath(path) && chatEnabled(userStatus.get().status);
      tabSheets.close();
      if (other == null) {
        if (path == null) return;
        webTabs.open(chat ? 'chat' : 'pages', path);
        router.dismissTo(chat ? '/chat' : '/pages');
        return;
      }
      void askToSwitch(other, timesheets.getState().timer != null).then((switched) => {
        if (!switched || path == null) return;
        webTabs.afterReset(authController.getState().generation, () => {
          webTabs.open(chat ? 'chat' : 'pages', path);
          router.dismissTo(chat ? '/chat' : '/pages');
        });
      });
    };
    const service = new PushService(api, expoPush, {
      production: !__DEV__,
      others: async () => (await authController.otherSessions()).map(authController.clientFor),
      onTap: (tap) =>
        void authController
          .sessions()
          .then((sessions) => openLink(tap.link, pushTarget(tap, sessions, authController.getState().session))),
      onReceived: (push) =>
        void authController.sessions().then((sessions) => {
          const other = pushTarget(push, sessions, authController.getState().session);
          if (other) otherServers.pushed(other, push.badge);
          else userStatus.pushed(push.badge);
        }),
    });
    void service.start().catch(() => {});
    return () => {
      service.stop();
      otherServers.stop();
    };
  }, [auth.generation, router]);

  // The number on the icon follows the notifications, of all logins.
  const openNotifications = status?.notifications?.openCount;
  const others = useOtherServers();
  const otherNotifications = others.reduce((sum, it) => sum + it.openCount, 0);
  useEffect(() => {
    if (Platform.OS === 'ios' && openNotifications != null) {
      expoPush.setBadge(openNotifications + otherNotifications).catch(() => {});
    }
  }, [openNotifications, otherNotifications]);

  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: colors.background }} edges={['top', 'left', 'right']}>
      {issue && issueKey !== confirmedIssue && (
        <CompatibilityBanner issue={issue} onConfirm={() => setConfirmedIssue(issueKey)} />
      )}
      <View style={styles.fill}>
        <Tabs
          key={auth.generation}
          screenOptions={{
            headerShown: false,
            sceneStyle: { backgroundColor: colors.background },
            tabBarPosition: rail ?? 'bottom',
          }}
          tabBar={(props) => <TabBar {...props} issue={issue} rail={rail} onSize={setBar} />}
        >
          <Tabs.Screen name="cards" />
          <Tabs.Screen name="addresses" />
          <Tabs.Screen name="timesheets" />
          <Tabs.Screen name="pages" />
          <Tabs.Screen name="chat" />
        </Tabs>
        {/* Outside the tabs of the login: the notifications stay open after switching to another one. */}
        <View
          style={[StyleSheet.absoluteFill, rail ? { [rail]: bar.width } : { bottom: bar.height }]}
          pointerEvents="box-none"
        >
          <NotificationsSheet
            onOpenLink={(path) => {
              const chat = isChatPath(path) && chatEnabled(userStatus.get().status);
              webTabs.open(chat ? 'chat' : 'pages', path);
              router.navigate(chat ? '/chat' : '/pages');
            }}
            onOtherServer={(session) => void askToSwitch(session, timesheets.getState().timer != null)}
          />
          <MoreMenu issue={issue} />
          <ServerSwitchSheet timerRunning={() => timesheets.getState().timer != null} />
          <SettingsSheet />
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  fill: { flex: 1 },
});
