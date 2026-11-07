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
import { usePathname, useRouter } from 'expo-router';
import { ComponentProps } from 'react';
import { Alert, StyleSheet, View } from 'react-native';
import { GestureDetector } from 'react-native-gesture-handler';

import { authController } from '../../core/auth/authController';
import { CompatibilityIssue } from '../../core/compat/compatibility';
import { t } from '../../core/i18n/translations';
import { appVersion } from '../../core/platform';
import { useOtherServers } from '../../core/status/otherServers';
import { useColors } from '../../core/ui/colors';
import { MenuItem, MenuList } from '../../core/ui/MenuSheet';
import { tabSheets, useTabSheet } from '../../core/ui/tabSheets';
import { TabSheet } from '../../core/ui/TabSheet';
import { useWebTabStates, WebTab, webTabs } from '../web/webTabs';
import { compatibilityText } from './TabBar';

type IconName = ComponentProps<typeof Ionicons>['name'];

/** The web tab of a path of the tabs (`/pages`, `/chat`). */
const webTabOf = (pathname: string): WebTab | undefined =>
  pathname === '/pages' ? 'pages' : pathname === '/chat' ? 'chat' : undefined;

/**
 * The "more" menu of the tab bar, a sheet moved up over the tabs (the tab bar stays below it): on a web tab the browser
 * functions first, then the account, the settings and the logout (as in the Flutter app). It counts the open
 * notifications of the other logins (their servers push too) at the server switch.
 */
export function MoreMenu({ issue }: { issue: CompatibilityIssue | null }) {
  const colors = useColors();
  const router = useRouter();
  const open = useTabSheet() === 'more';
  const webTab = webTabOf(usePathname());
  const webStates = useWebTabStates();
  const otherOpen = useOtherServers().reduce((sum, it) => sum + it.openCount, 0);

  const browserItems = (): MenuItem[] => {
    const handle = webTab ? webTabs.handle(webTab) : undefined;
    const web = webTab ? webStates[webTab] : undefined;
    return [
      { key: 'back', label: t('back'), icon: 'arrow-back', disabled: !web?.canGoBack, onPress: () => handle?.goBack() },
      { key: 'forward', label: t('app.web.forward'), icon: 'arrow-forward', disabled: !web?.canGoForward, onPress: () => handle?.goForward() },
      { key: 'reload', label: t('app.web.reload'), icon: 'refresh', onPress: () => handle?.reload() },
      { key: 'find', label: t('app.web.find'), icon: 'search', onPress: () => handle?.openFind() },
      { key: 'share', label: t('app.web.share'), icon: 'share-outline', disabled: !web?.hasPage, onPress: () => handle?.share() },
      { key: 'browser', label: t('app.web.openInBrowser'), icon: 'open-outline', disabled: !web?.hasPage, onPress: () => handle?.openInBrowser() },
      { key: 'restart', label: t('app.web.restart'), icon: 'reload-circle-outline', onPress: () => handle?.restart() },
    ];
  };

  /** The user and the server of the login, to read only. */
  const accountItems = (): MenuItem[] => {
    const session = authController.getState().session;
    if (session == null) return [];
    return [{ key: 'account', label: session.fullname ?? session.username ?? '', detail: session.serverUrl, info: true }];
  };

  const items = (): MenuItem[] => [
    ...(webTab ? browserItems() : []),
    ...accountItems(),
    { key: 'version', label: t('app.version', appVersion), info: true },
    ...(issue ? [{ key: 'compatibility', label: compatibilityText(issue), icon: 'warning-outline' as IconName, info: true }] : []),
    {
      key: 'servers',
      label: t('app.server.switch'),
      // The badge of the "more" button: the open notifications of the other logins, said here what it counts.
      detail: otherOpen > 0 ? t('app.server.switch.notifications', otherOpen) : undefined,
      badge: otherOpen,
      icon: 'swap-horizontal',
      onPress: () => router.push('/servers'),
    },
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
    <TabSheet open={open} fit closeLabel={t('close')} onClose={tabSheets.close}>
      {(pull) => (
        <View style={[styles.content, { backgroundColor: colors.surface }]}>
          <GestureDetector gesture={pull}>
            <View style={styles.handleArea}>
              <View style={[styles.handle, { backgroundColor: colors.textMuted }]} />
            </View>
          </GestureDetector>
          <MenuList
            items={items()}
            onChoose={(item) => {
              // No modal to wait for (a share sheet, an alert): the action runs at once.
              tabSheets.close();
              item.onPress?.();
            }}
          />
        </View>
      )}
    </TabSheet>
  );
}

const styles = StyleSheet.create({
  content: { flexShrink: 1, borderTopLeftRadius: 14, borderTopRightRadius: 14, overflow: 'hidden', paddingBottom: 8 },
  handleArea: { alignItems: 'center', paddingTop: 6, paddingBottom: 6 },
  handle: { width: 36, height: 5, borderRadius: 3, opacity: 0.5 },
});
