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

import { Platform } from 'react-native';

import PfNative from '../../../modules/pf-native/src/PfNativeModule';
import { SharedFiles } from '../files/sharedFiles';

/** Apple Wallet passes exist on the iPhone only. */
export const walletAvailable = Platform.OS === 'ios';

/** A pass the Wallet refused or couldn't show (e.g. an invalid signature), with the reason of iOS. */
export class WalletException extends Error {}

/**
 * Shows the system sheet of iOS to add the pass (the bytes of a `.pkpass` file) to the Wallet.
 *
 * Throws [WalletException] if iOS refuses the pass.
 */
export async function showWalletPass(pass: Uint8Array): Promise<void> {
  const file = SharedFiles.write('pass.pkpass', pass);
  try {
    await PfNative.addPass(file.uri);
  } catch (e) {
    throw new WalletException(e instanceof Error ? e.message : String(e));
  } finally {
    // The sheet has read the pass already.
    if (file.exists) file.delete();
  }
}
