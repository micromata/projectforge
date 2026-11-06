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

import { Ionicons } from '@expo/vector-icons';
import { Tabs, useRouter } from 'expo-router';
import { ComponentProps, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { appVersion } from '../../core/platform';
import { authController } from '../../core/auth/authController';
import { CompatibilityIssue } from '../../core/compat/compatibility';
import { t } from '../../core/i18n/translations';
import { chatEnabled, userStatus, useUserStatus } from '../../core/status/userStatus';
import { severityColor, useColors } from '../../core/ui/colors';
import { MenuItem, MenuSheet } from '../../core/ui/MenuSheet';
import { TIMESHEETS_TAB_ENABLED } from '../../core/web/nativePage';
import { CHAT_PATH, useWebTabStates, WebTab, webTabs } from '../web/webTabs';

type IconName = ComponentProps<typeof Ionicons>['name'];

/** The props of a tab bar of expo-router's Tabs (its React Navigation is bundled, not a dependency of its own). */
export type TabBarProps = Parameters<NonNullable<ComponentProps<typeof Tabs>['tabBar']>>[0];

/** The tabs of the app: route name → icon, label and the web tab it shows (if it is one). */
export const TABS: Record<string, { icon: IconName; label: () => string; webTab?: WebTab }> = {
  cards: { icon: 'id-card-outline', label: () => t('menu.myBusinessCards') },
  addresses: { icon: 'people-outline', label: () => t('menu.addressList') },
  timesheets: { icon: 'time-outline', label: () => t('timesheet.title.heading') },
  pages: { icon: 'globe-outline', label: () => t('app.web'), webTab: 'pages' },
  chat: { icon: 'chatbubbles-outline', label: () => t('chat.title'), webTab: 'chat' },
};

export function compatibilityText(issue: CompatibilityIssue): string {
  return issue.kind === 'appTooOld'
    ? t('app.compatibility.appTooOld', issue.minVersion)
    : t('app.compatibility.serverTooOld', issue.minVersion);
}

/**
 * The navigation of the app: slim, icons only (the label is their accessibility label), with the browser functions of
 * a web tab and the menu of the account at its end, so they are there on every tab (the pages bring their own header).
 * The chat tab is missing if the chat is switched off on the server; tapped again it leads back to its rooms.
 */
export function TabBar({ state, navigateToTab, issue }: TabBarProps & { issue: CompatibilityIssue | null }) {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { status } = useUserStatus();
  const webStates = useWebTabStates();
  const [menu, setMenu] = useState<'browser' | 'more' | null>(null);
  const focused = state.routes[state.index]?.name;
  const webTab = focused ? TABS[focused]?.webTab : undefined;
  const unread = status?.chatUnread ?? 0;
  const openNotifications = status?.notifications?.openCount ?? 0;

  const routes = state.routes.filter(
    (route) =>
      TABS[route.name] &&
      (route.name !== 'chat' || chatEnabled(status)) &&
      (route.name !== 'timesheets' || TIMESHEETS_TAB_ENABLED),
  );

  const browserItems = (): MenuItem[] => {
    const handle = webTab ? webTabs.handle(webTab) : undefined;
    const web = webTab ? webStates[webTab] : undefined;
    return [
      { key: 'back', label: t('back'), icon: 'arrow-back', disabled: !web?.canGoBack, onPress: () => handle?.goBack() },
      { key: 'forward', label: t('app.web.forward'), icon: 'arrow-forward', disabled: !web?.canGoForward, onPress: () => handle?.goForward() },
      { key: 'reload', label: t('app.web.reload'), icon: 'refresh', onPress: () => handle?.reload() },
      { key: 'find', label: t('app.web.find'), icon: 'search', onPress: () => handle?.openFind() },
      { key: 'share', label: t('app.web.share'), icon: 'share-outline', onPress: () => handle?.share() },
      { key: 'browser', label: t('app.web.openInBrowser'), icon: 'open-outline', onPress: () => handle?.openInBrowser() },
      { key: 'restart', label: t('app.web.restart'), icon: 'reload-circle-outline', onPress: () => handle?.restart() },
    ];
  };

  const moreItems = (): MenuItem[] => [
    { key: 'version', label: t('app.version', appVersion), info: true },
    ...(issue ? [{ key: 'compatibility', label: compatibilityText(issue), icon: 'warning-outline' as IconName, info: true }] : []),
    { key: 'servers', label: t('app.server.switch'), icon: 'swap-horizontal', onPress: () => router.push('/servers') },
    { key: 'settings', label: t('settings'), icon: 'settings-outline', onPress: () => router.push('/settings') },
    {
      key: 'logout',
      label: t('menu.logout'),
      icon: 'log-out-outline',
      destructive: true,
      onPress: () =>
        Alert.alert(t('menu.logout'), t('app.logout.question'), [
          { text: t('cancel'), style: 'cancel' },
          { text: t('ok'), style: 'destructive', onPress: () => void authController.logout() },
        ]),
    },
  ];

  return (
    <View style={[styles.bar, { backgroundColor: colors.surface, borderColor: colors.border, paddingBottom: insets.bottom }]}>
      {routes.map((route) => {
        const tab = TABS[route.name];
        const active = route.name === focused;
        return (
          <Pressable
            key={route.key}
            style={styles.item}
            accessibilityRole="tab"
            accessibilityLabel={tab.label()}
            accessibilityState={{ selected: active }}
            onPress={() => {
              if (active && route.name === 'chat') {
                webTabs.open('chat', CHAT_PATH);
                return;
              }
              if (!active) navigateToTab(route.key);
              // Leaving the chat: the badge at once, not after the next poll.
              if (!active && focused === 'chat') void userStatus.refresh();
            }}
          >
            <Ionicons name={tab.icon} size={24} color={active ? colors.primary : colors.textMuted} />
            {route.name === 'chat' && unread > 0 && (
              <View style={[styles.badge, { backgroundColor: colors.badge }]}>
                <Text style={styles.badgeText}>{unread > 99 ? '99+' : unread}</Text>
              </View>
            )}
          </Pressable>
        );
      })}
      <View style={styles.spacer} />
      {webTab && (
        <Pressable style={styles.item} accessibilityLabel={t('app.menu.more')} onPress={() => setMenu('browser')}>
          <Ionicons name="compass-outline" size={24} color={colors.textMuted} />
        </Pressable>
      )}
      <Pressable
        style={styles.item}
        accessibilityLabel={t('notification.center')}
        onPress={() => router.push('/notifications')}
      >
        <Ionicons
          name={openNotifications > 0 ? 'notifications' : 'notifications-outline'}
          size={24}
          color={colors.textMuted}
        />
        {openNotifications > 0 && (
          <View
            style={[
              styles.badge,
              { backgroundColor: severityColor(colors, status?.notifications?.maxSeverity) ?? colors.badge },
            ]}
          >
            <Text style={styles.badgeText}>{openNotifications > 99 ? '99+' : openNotifications}</Text>
          </View>
        )}
      </Pressable>
      <Pressable style={styles.item} accessibilityLabel={t('app.menu.more')} onPress={() => setMenu('more')}>
        <Ionicons name="ellipsis-horizontal-circle-outline" size={24} color={issue ? colors.danger : colors.textMuted} />
      </Pressable>
      <MenuSheet
        visible={menu != null}
        items={menu === 'browser' ? browserItems() : menu === 'more' ? moreItems() : []}
        onClose={() => setMenu(null)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', borderTopWidth: StyleSheet.hairlineWidth, paddingHorizontal: 4 },
  item: { height: 44, minWidth: 52, alignItems: 'center', justifyContent: 'center' },
  spacer: { flex: 1 },
  badge: {
    position: 'absolute',
    top: 4,
    right: 8,
    minWidth: 18,
    height: 18,
    borderRadius: 9,
    paddingHorizontal: 4,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { color: '#fff', fontSize: 11, fontWeight: '600' },
});
