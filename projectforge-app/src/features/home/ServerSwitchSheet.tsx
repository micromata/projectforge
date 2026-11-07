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

import { useState } from 'react';

import { t } from '../../core/i18n/translations';
import { LogScreen } from '../../core/log/LogScreen';
import { tabSheets, useTabSheet } from '../../core/ui/tabSheets';
import { TitledTabSheet } from '../../core/ui/TitledTabSheet';
import { LoginScreen } from '../login/LoginScreen';
import { ServerSwitchScreen } from './ServerSwitchScreen';

/** The pages of the sheet: the logins, the login at another server, the log (from that login). */
type Page = 'servers' | 'add' | 'log';

/**
 * The server switch of the "more" menu, a sheet moved up over the tabs (the tab bar stays below it). The login at
 * another server (and its log) opens within the sheet, the arrow beside the title goes back; a switch or a new login
 * closes it. Opened again, the sheet starts with the logins.
 *
 * @param timerRunning Whether the timer of the time sheets runs: discarded by a switch, so the user is asked first.
 */
export function ServerSwitchSheet({ timerRunning }: { timerRunning: () => boolean }) {
  const open = useTabSheet() === 'servers';
  const [page, setPage] = useState<Page>('servers');
  const [wasOpen, setWasOpen] = useState(open);
  if (open !== wasOpen) {
    setWasOpen(open);
    if (open) setPage('servers');
  }
  const title = page === 'log' ? t('app.log.title') : page === 'add' ? t('app.server.add') : t('app.server.switch');
  return (
    <TitledTabSheet
      open={open}
      title={title}
      onClose={tabSheets.close}
      onBack={page === 'log' ? () => setPage('add') : page === 'add' ? () => setPage('servers') : undefined}
    >
      {page === 'servers' && (
        <ServerSwitchScreen onDone={tabSheets.close} onAdd={() => setPage('add')} timerRunning={timerRunning} />
      )}
      {page === 'add' && <LoginScreen addServer onDone={tabSheets.close} onShowLog={() => setPage('log')} />}
      {page === 'log' && <LogScreen />}
    </TitledTabSheet>
  );
}
