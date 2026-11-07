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

import { Alert } from 'react-native';

import { authController } from '../../core/auth/authController';
import { Session } from '../../core/auth/session';
import { t } from '../../core/i18n/translations';
import { parseUrl } from '../../core/url';

/** The server of [session] as shown to the user: its host. */
export const serverName = (session: Session) => parseUrl(session.serverUrl)?.host ?? session.serverUrl;

/** True without a running timer, otherwise if the user agrees to discard it (the switch clears the data of the app). */
export function timerMayBeDiscarded(timerRunning: boolean): Promise<boolean> {
  if (!timerRunning) return Promise.resolve(true);
  return new Promise((resolve) =>
    Alert.alert('', t('app.server.switch.timer'), [
      { text: t('cancel'), style: 'cancel', onPress: () => resolve(false) },
      { text: t('app.server.switch'), style: 'destructive', onPress: () => resolve(true) },
    ]),
  );
}

/**
 * Asks whether to switch to the stored login [session] (something of another server was chosen: a push, its
 * notifications) and switches; true if switched. A running timer is confirmed as by the server switch.
 */
export async function askToSwitch(session: Session, timerRunning: boolean): Promise<boolean> {
  const agreed = await new Promise<boolean>((resolve) =>
    Alert.alert(t('app.server.switch'), t('app.server.switch.question', serverName(session)), [
      { text: t('cancel'), style: 'cancel', onPress: () => resolve(false) },
      { text: t('app.server.switch'), onPress: () => resolve(true) },
    ]),
  );
  if (!agreed || !(await timerMayBeDiscarded(timerRunning))) return false;
  await authController.switchTo(session);
  return true;
}
