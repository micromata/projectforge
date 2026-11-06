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

import { useCallback } from 'react';

import { useIsFocused, useRouter } from 'expo-router';
import { appSettings, opensInApp } from '../../core/settings/appSettings';
import { TIMESHEETS_TAB_ENABLED } from '../../core/web/nativePage';
import { chatEnabled, userStatus } from '../../core/status/userStatus';
import { WebScreen } from '../../features/web/WebScreen';
import { NativePage } from '../../features/web/webNavigation';
import { webTabs } from '../../features/web/webTabs';

/** The pages of the server (next, and the legacy pages it links to). */
export default function PagesTab() {
  const router = useRouter();
  const focused = useIsFocused();
  const register = useCallback((handle: Parameters<typeof webTabs.register>[1]) => webTabs.register('pages', handle), []);
  return (
    <WebScreen
      ref={register}
      initialPath={webTabs.takePending('pages')}
      active={focused}
      onStateChange={(state) => webTabs.setState('pages', state)}
      onNativePage={(page: NativePage) => {
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
      }}
    />
  );
}
