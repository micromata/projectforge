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

import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';

import 'log_sanitizer.dart';

enum LogLevel { debug, info, warn, error }

class LogEntry {
  const LogEntry(this.time, this.level, this.tag, this.message);

  final DateTime time;
  final LogLevel level;

  /// The area: `api`, `login`, `auth`, `web`, `app`.
  final String tag;
  final String message;

  @override
  String toString() =>
      '${time.toIso8601String()} ${level.name.toUpperCase().padRight(5)} '
      '[$tag] ${message.replaceAll('\n', '\n    ')}';
}

/// The log of the app (login, requests, WebView navigation), shown and copied in the settings to find out why
/// something fails, e.g. behind an SSO proxy.
///
/// - Only in memory, never written to a file; at most [maxAge] old and at most [maxEntries] entries.
/// - Every message goes through [LogSanitizer.text]: no tokens, cookie values, codes or passwords. The log points
///   pass urls and cookies through [LogSanitizer] themselves.
class AppLog extends ChangeNotifier {
  AppLog({DateTime Function()? clock, this.maxEntries = 2000})
    : _clock = clock ?? DateTime.now;

  static const maxAge = Duration(days: 1);

  final DateTime Function() _clock;
  final int maxEntries;
  final _entries = <LogEntry>[];

  /// The entries, oldest first.
  List<LogEntry> get entries {
    _prune();
    return List.unmodifiable(_entries);
  }

  void debug(String tag, String message) => add(LogLevel.debug, tag, message);

  void info(String tag, String message) => add(LogLevel.info, tag, message);

  void warn(String tag, String message) => add(LogLevel.warn, tag, message);

  void error(String tag, String message, [Object? error, StackTrace? stack]) {
    final details = StringBuffer(message);
    if (error != null) details.write(': ${error.runtimeType}: $error');
    if (stack != null) {
      // The first frames are enough to locate it.
      details.write('\n${stack.toString().split('\n').take(8).join('\n')}');
    }
    add(LogLevel.error, tag, details.toString());
  }

  void add(LogLevel level, String tag, String message) {
    final entry = LogEntry(_clock(), level, tag, LogSanitizer.text(message));
    _entries.add(entry);
    _prune();
    if (_print) debugPrint('PF $entry');
    _notify();
  }

  void clear() {
    _entries.clear();
    _notify();
  }

  /// Also on the console in a debug build, not in the tests.
  static final _print =
      kDebugMode && !Platform.environment.containsKey('FLUTTER_TEST');

  bool _notifying = false;

  /// Deferred: an entry may be written while a widget builds (e.g. an error of the framework), the log screen must
  /// not rebuild then. Several entries in a row notify once.
  void _notify() {
    if (_notifying) return;
    _notifying = true;
    scheduleMicrotask(() {
      _notifying = false;
      notifyListeners();
    });
  }

  /// All entries as text, for copying. Further lines of an entry (a stack trace, a text of the server) are
  /// indented: only the first line of an entry starts with a time.
  String text() => entries.join('\n');

  void _prune() {
    final oldest = _clock().subtract(maxAge);
    _entries.removeWhere((it) => it.time.isBefore(oldest));
    final excess = _entries.length - maxEntries;
    if (excess > 0) _entries.removeRange(0, excess);
  }
}

/// The log of the app; global, so the api client and the login reach it without a Riverpod ref.
final appLog = AppLog();
