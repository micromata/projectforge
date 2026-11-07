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

import { useRouter } from 'expo-router';

import { chatEnabled, userStatus } from '../core/status/userStatus';
import { isChatPath } from '../core/web/links';
import { askToSwitch } from '../features/home/switchServer';
import { NotificationsScreen } from '../features/notifications/NotificationsScreen';
import { timesheets } from '../features/timesheets/timesheetsStore';
import { webTabs } from '../features/web/webTabs';

/**
 * The notifications, over the tabs; a link is shown in the chat tab (a page of the chat) or in the pages. Another
 * login's open notifications: after asking, switched there (the list stays open, now of that server).
 */
export default function Notifications() {
  const router = useRouter();
  return (
    <NotificationsScreen
      onOpenLink={(path) => {
        const chat = isChatPath(path) && chatEnabled(userStatus.get().status);
        webTabs.open(chat ? 'chat' : 'pages', path);
        router.dismissTo(chat ? '/chat' : '/pages');
      }}
      onOtherServer={(session) => void askToSwitch(session, timesheets.getState().timer != null)}
    />
  );
}
