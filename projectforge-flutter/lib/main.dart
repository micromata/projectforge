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

import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app.dart';
import 'core/i18n/translations.dart';
import 'core/log/app_log.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  _logUncaughtErrors();
  final translations = await Translations.load();
  // The names of the days and months in the language of the texts.
  await initializeDateFormatting(translations.locale);
  runApp(
    ProviderScope(
      // A failed load is shown with a button to try again. Riverpod's own retries (ten, with growing pauses) would
      // keep the spinner up for a minute first.
      retry: (_, _) => null,
      child: ProjectForgeApp(translations: translations),
    ),
  );
}

/// Errors nobody caught (of the framework and of asynchronous code) go into the client log, in addition to the
/// default handling.
void _logUncaughtErrors() {
  final presentError = FlutterError.onError;
  FlutterError.onError = (details) {
    appLog.error('app', 'Flutter error', details.exception, details.stack);
    presentError?.call(details);
  };
  final platformError = PlatformDispatcher.instance.onError;
  PlatformDispatcher.instance.onError = (error, stack) {
    appLog.error('app', 'Uncaught error', error, stack);
    return platformError?.call(error, stack) ?? false;
  };
  appLog.info('app', 'Started');
}
