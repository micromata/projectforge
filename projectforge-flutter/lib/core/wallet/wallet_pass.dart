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

import 'dart:io';

import 'package:flutter/services.dart';

const _channel = MethodChannel('projectforge/wallet');

/// Apple Wallet passes exist on the iPhone only.
bool get walletAvailable => Platform.isIOS;

/// A pass the Wallet refused or couldn't show (e.g. an invalid signature), with the reason of iOS.
class WalletException implements Exception {
  WalletException(this.message);

  final String message;
}

/// Shows the system sheet of iOS to add the pass (the bytes of a `.pkpass` file) to the Wallet.
///
/// Throws [WalletException] if iOS refuses the pass.
Future<void> showWalletPass(List<int> pass) async {
  try {
    await _channel.invokeMethod<void>('addPass', Uint8List.fromList(pass));
  } on PlatformException catch (e) {
    throw WalletException(e.message ?? e.code);
  }
}
