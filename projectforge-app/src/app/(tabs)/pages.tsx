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

import { useCallback, useMemo } from 'react';
import { Linking, StyleSheet, View } from 'react-native';

import { useIsFocused, useRouter } from 'expo-router';
import { authController } from '../../core/auth/authController';
import { appLog } from '../../core/log/appLog';
import { appSettings, opensInApp } from '../../core/settings/appSettings';
import { TIMESHEETS_TAB_ENABLED } from '../../core/web/nativePage';
import { chatEnabled, userStatus } from '../../core/status/userStatus';
import { WebMenu } from '../../features/web/WebMenu';
import { WebScreen } from '../../features/web/WebScreen';
import { NativePage, WebNavigation } from '../../features/web/webNavigation';
import { useWebMenuOpen, webTabs } from '../../features/web/webTabs';

/**
 * The pages of the server (next, and the legacy pages it links to), with the menu over them when the tab is chosen
 * (the globe of the tab bar opens and closes it). The page loads below the menu, closing it shows the page as it was.
 */
export default function PagesTab() {
  const router = useRouter();
  const focused = useIsFocused();
  const menuOpen = useWebMenuOpen();
  const register = useCallback(
    (handle: Parameters<typeof webTabs.register>[1]) => webTabs.register('pages', handle),
    [],
  );
  const serverUrl = authController.getState().session?.serverUrl;
  const navigation = useMemo(() => (serverUrl ? new WebNavigation(serverUrl) : null), [serverUrl]);

  /** True if the app shows [page] in a tab (or screen) of its own. */
  const showNative = (page: NativePage): boolean => {
    if (!opensInApp(appSettings.get(), page)) return false;
    if (page === 'chat' && chatEnabled(userStatus.get().status)) {
      router.navigate('/chat');
      return true;
    }
    if (page === 'businessCards' || page === 'addresses' || (page === 'timesheets' && TIMESHEETS_TAB_ENABLED)) {
      router.navigate(page === 'businessCards' ? '/cards' : page === 'addresses' ? '/addresses' : '/timesheets');
      return true;
    }
    if (page === 'notifications') {
      // No tab: the list lies over the tabs, like the bell opens it.
      router.push('/notifications');
      return true;
    }
    return false;
  };

  /** An entry of the menu: a page with a tab of its own there, another site in the system browser, else here. */
  const openEntry = (path: string) => {
    if (navigation == null) return;
    const url = navigation.pageUrl(path);
    if (url == null) {
      if (navigation.decide(path) === 'external') {
        void Linking.openURL(path).catch((e) => appLog.warn('web', `Not opened outside the app: ${String(e)}`));
      }
      return;
    }
    const page = navigation.nativePage(url);
    if (page != null && showNative(page)) return;
    webTabs.open('pages', path);
  };

  return (
    <View style={styles.fill}>
      <WebScreen
        ref={register}
        initialPath={webTabs.takePending('pages')}
        // Under the menu as well: it loads the page (and renews a login refused by the SSO proxy) meanwhile.
        active={focused}
        onStateChange={(state) => webTabs.setState('pages', state)}
        onNativePage={showNative}
      />
      <WebMenu open={menuOpen} active={focused} onOpen={openEntry} onClose={closeMenu} />
    </View>
  );
}

const closeMenu = () => webTabs.setMenu(false);

const styles = StyleSheet.create({
  fill: { flex: 1 },
});
