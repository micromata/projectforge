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
import { SettingsScreen } from './SettingsScreen';

/**
 * The settings of the "more" menu, a sheet moved up over the tabs (the tab bar stays below it). The log opens within
 * the sheet, the arrow beside the title goes back; opened again, the sheet starts with the settings.
 */
export function SettingsSheet() {
  const open = useTabSheet() === 'settings';
  const [log, setLog] = useState(false);
  const [wasOpen, setWasOpen] = useState(open);
  if (open !== wasOpen) {
    setWasOpen(open);
    if (open) setLog(false);
  }
  return (
    <TitledTabSheet
      open={open}
      title={log ? t('app.log.title') : t('settings')}
      onClose={tabSheets.close}
      onBack={log ? () => setLog(false) : undefined}
    >
      {log ? <LogScreen /> : <SettingsScreen onShowLog={() => setLog(true)} />}
    </TitledTabSheet>
  );
}
