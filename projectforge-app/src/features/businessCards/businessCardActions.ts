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

import { SharedFiles } from '../../core/files/sharedFiles';
import { errorText } from '../../core/i18n/translations';
import { appLog } from '../../core/log/appLog';
import { showWalletPass } from '../../core/wallet/walletPass';
import { BusinessCard, vCardFileName } from './businessCards';
import { businessCards } from './businessCardsStore';

/** Shares the card as vCard file (mail, messenger, AirDrop, …); a failure is reported in an alert. */
export async function shareVCard(card: BusinessCard): Promise<void> {
  await report('vCard', async () => {
    const repository = businessCards.repository();
    if (repository == null || !card.id) return;
    await SharedFiles.share(vCardFileName(card), await repository.vCard(card.id), 'text/vcard');
  });
}

/** Loads the Wallet pass of the card and shows the system sheet of iOS to add it; a failure is reported in an alert. */
export async function addToWallet(card: BusinessCard): Promise<void> {
  await report('Wallet pass', async () => {
    const repository = businessCards.repository();
    if (repository == null || !card.id) return;
    await showWalletPass(await repository.pass(card.id));
  });
}

async function report(what: string, action: () => Promise<void>) {
  try {
    await action();
  } catch (e) {
    appLog.warn('businessCards', `${what} failed: ${String(e)}`);
    Alert.alert(errorText(e));
  }
}
