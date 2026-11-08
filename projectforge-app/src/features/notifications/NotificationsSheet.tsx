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

import { Session } from '../../core/auth/session';
import { t } from '../../core/i18n/translations';
import { tabSheets, useTabSheet } from '../../core/ui/tabSheets';
import { TitledTabSheet } from '../../core/ui/TitledTabSheet';
import { NotificationsScreen } from './NotificationsScreen';

/**
 * The notifications of the bell, a sheet moved up over the tabs (the tab bar stays below it), pulled down by its
 * title. [onOpenLink] shows the page of a link (the sheet closes), [onOtherServer] switches to another login with open
 * notifications (the sheet stays, now with those of that server).
 */
export function NotificationsSheet({
  onOpenLink,
  onOtherServer,
}: {
  onOpenLink: (path: string) => void;
  onOtherServer: (session: Session) => void;
}) {
  const open = useTabSheet() === 'notifications';
  return (
    <TitledTabSheet open={open} title={t('notification.center')} onClose={tabSheets.close}>
      <NotificationsScreen
        onOpenLink={(path) => {
          tabSheets.close();
          onOpenLink(path);
        }}
        onOtherServer={onOtherServer}
      />
    </TitledTabSheet>
  );
}
