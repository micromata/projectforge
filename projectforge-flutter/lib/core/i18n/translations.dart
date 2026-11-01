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

import 'dart:convert';
import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

/// The texts of the app: the catalog generated from the I18nResources bundles of the server
/// (`assets/i18n/generated.<locale>.json`, see GenerateFlutterI18nMessagesMain) with the hand-written texts of the
/// app (`assets/i18n/app.<locale>.json`) laid over it.
///
/// Keys are always written out literally (`t('businessCard.new')`): the generator exports only the keys it finds
/// in the sources.
class Translations {
  Translations(this.locale, this._messages);

  static const supportedLocales = ['en', 'de'];

  final String locale;
  final Map<String, String> _messages;

  /// The text of [key] with the placeholders `{0}`, `{1}`, … replaced by [args]. A missing key is shown as the key
  /// itself, so it is spotted instead of hidden.
  String t(String key, [List<Object?> args = const []]) {
    var text = _messages[key] ?? key;
    for (var i = 0; i < args.length; i++) {
      text = text.replaceAll('{$i}', '${args[i] ?? ''}');
    }
    return text;
  }

  /// Loads the catalog of the device's language, English if it isn't supported.
  static Future<Translations> load([Locale? deviceLocale]) async {
    final language =
        (deviceLocale ?? PlatformDispatcher.instance.locale).languageCode;
    final locale = supportedLocales.contains(language) ? language : 'en';
    final messages = <String, String>{};
    for (final name in ['generated', 'app']) {
      final json = await rootBundle.loadString(
        'assets/i18n/$name.$locale.json',
      );
      (jsonDecode(json) as Map<String, dynamic>).forEach((key, value) {
        if (!key.startsWith('_')) messages[key] = value as String;
      });
    }
    return Translations(locale, messages);
  }
}

/// Makes the [Translations] available to the widget tree, read via `context.t('key')`.
class TranslationsScope extends InheritedWidget {
  const TranslationsScope({
    super.key,
    required this.translations,
    required super.child,
  });

  final Translations translations;

  static Translations of(BuildContext context) => context
      .dependOnInheritedWidgetOfExactType<TranslationsScope>()!
      .translations;

  @override
  bool updateShouldNotify(TranslationsScope oldWidget) =>
      translations != oldWidget.translations;
}

extension TranslationsContext on BuildContext {
  String t(String key, [List<Object?> args = const []]) =>
      TranslationsScope.of(this).t(key, args);
}
