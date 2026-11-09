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
import { Tabs } from 'expo-router';
import { ComponentProps, useEffect } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { CompatibilityIssue } from '../../core/compat/compatibility';
import { APP_TABS, appSettings, RailSide } from '../../core/settings/appSettings';
import { t } from '../../core/i18n/translations';
import { useOtherServers } from '../../core/status/otherServers';
import { chatEnabled, userStatus, useUserStatus } from '../../core/status/userStatus';
import { severityColor, useColors } from '../../core/ui/colors';
import { buttonOf, tabSheets, useTabSheet } from '../../core/ui/tabSheets';
import { TIMESHEETS_TAB_ENABLED } from '../../core/web/nativePage';
import { CHAT_PATH, useWebMenuOpen, WebTab, webTabs } from '../web/webTabs';

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
 * The navigation of the app: slim, icons only (the label is their accessibility label), with the bell and the "more"
 * menu at its end (on a web tab with the browser functions first), so they are there on every tab (the pages bring
 * their own header). Both open a sheet over the tabs ([tabSheets]), the bar stays below it (also below the settings and
 * the server switch of the menu); the button again or a tab closes it. The "more" button counts the open notifications of the other logins (their servers push too).
 * The chat tab is missing if the chat is switched off on the server; tapped again it leads back to its rooms. The
 * pages tab opens with its menu, tapped again it closes or opens it.
 * [rail]: in landscape a column at this side (as the Flutter app's navigation rail; left or right as chosen in the
 * settings), the bell and the menu at its bottom. [onSize]: the size of the bar, the sheets leave it free.
 */
export function TabBar({
  state,
  navigateToTab,
  issue,
  rail = null,
  onSize,
}: TabBarProps & {
  issue: CompatibilityIssue | null;
  rail?: RailSide | null;
  onSize?: (size: { width: number; height: number }) => void;
}) {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { status } = useUserStatus();
  const webMenu = useWebMenuOpen();
  const sheet = buttonOf(useTabSheet());
  const focused = state.routes[state.index]?.name;
  const unread = status?.chatUnread ?? 0;
  const openNotifications = status?.notifications?.openCount ?? 0;
  // The other logins' open notifications, at the menu with the server switch.
  const otherOpen = useOtherServers().reduce((sum, it) => sum + it.openCount, 0);

  // The chat switched off on the server while it is shown: back to the business cards.
  const chatOff = focused === 'chat' && status != null && !chatEnabled(status);
  const cardsKey = state.routes.find((it) => it.name === 'cards')?.key;
  useEffect(() => {
    if (chatOff && cardsKey) navigateToTab(cardsKey);
  }, [chatOff, cardsKey, navigateToTab]);

  // The app starts with the tab shown last, also if it was reached by a link or a push.
  useEffect(() => {
    const tab = APP_TABS.find((it) => it === focused);
    if (tab) appSettings.setLastTab(tab);
  }, [focused]);

  const routes = state.routes.filter(
    (route) =>
      TABS[route.name] &&
      (route.name !== 'chat' || chatEnabled(status)) &&
      (route.name !== 'timesheets' || TIMESHEETS_TAB_ENABLED),
  );

  return (
    <View
      style={[
        rail ? [styles.rail, rail === 'left' ? styles.railLeft : styles.railRight] : styles.bar,
        { backgroundColor: colors.surface, borderColor: colors.border, paddingBottom: insets.bottom },
      ]}
      onLayout={(event) => onSize?.(event.nativeEvent.layout)}
    >
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
              // A sheet over the tabs closes, whichever tab is tapped.
              tabSheets.close();
              if (active && route.name === 'chat') {
                webTabs.open('chat', CHAT_PATH);
                return;
              }
              // The pages tab starts with its menu (the web pages' navigation is small on a phone); tapped again it
              // closes the menu, and opens it again over the page.
              if (route.name === 'pages') {
                if (active) {
                  webTabs.toggleMenu();
                  return;
                }
                webTabs.setMenu(true);
              }
              if (active) return;
              navigateToTab(route.key);
              // The badges at once (e.g. leaving the chat), not after the next poll.
              void userStatus.refresh();
            }}
          >
            <Ionicons
              name={route.name === 'pages' && active && webMenu ? 'globe' : tab.icon}
              size={24}
              color={active ? colors.primary : colors.textMuted}
            />
            {route.name === 'chat' && unread > 0 && (
              <View style={[styles.badge, { backgroundColor: colors.badge }]}>
                <Text style={styles.badgeText}>{unread > 99 ? '99+' : unread}</Text>
              </View>
            )}
          </Pressable>
        );
      })}
      <View style={styles.spacer} />
      <Pressable
        style={styles.item}
        accessibilityLabel={t('notification.center')}
        onPress={() => tabSheets.toggle('notifications')}
      >
        <Ionicons
          name={openNotifications > 0 ? 'notifications' : 'notifications-outline'}
          size={24}
          color={sheet === 'notifications' ? colors.primary : colors.textMuted}
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
      <Pressable style={styles.item} accessibilityLabel={t('app.menu.more')} onPress={() => tabSheets.toggle('more')}>
        <Ionicons
          name="ellipsis-horizontal-circle-outline"
          size={24}
          color={issue ? colors.danger : sheet === 'more' ? colors.primary : colors.textMuted}
        />
        {otherOpen > 0 && (
          <View style={[styles.badge, { backgroundColor: colors.badge }]}>
            <Text style={styles.badgeText}>{otherOpen > 99 ? '99+' : otherOpen}</Text>
          </View>
        )}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', alignItems: 'center', borderTopWidth: StyleSheet.hairlineWidth, paddingHorizontal: 4 },
  rail: { flexDirection: 'column', alignItems: 'center', paddingTop: 4 },
  railLeft: { borderRightWidth: StyleSheet.hairlineWidth },
  railRight: { borderLeftWidth: StyleSheet.hairlineWidth },
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
