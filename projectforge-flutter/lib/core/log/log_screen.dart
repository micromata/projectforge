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

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart';

import '../i18n/translations.dart';
import 'app_log.dart';

/// The client log ([AppLog]), newest first, to be copied e.g. into a support request.
class LogScreen extends StatelessWidget {
  const LogScreen({super.key, this.otherLog});

  /// Another log than [appLog] (tests).
  final AppLog? otherLog;

  AppLog get log => otherLog ?? appLog;

  static final _time = DateFormat('HH:mm:ss');

  Future<void> _copy(BuildContext context) async {
    final messenger = ScaffoldMessenger.of(context);
    final copied = context.t('app.log.copied');
    await Clipboard.setData(ClipboardData(text: log.text()));
    messenger.showSnackBar(SnackBar(content: Text(copied)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(context.t('app.log.title')),
        actions: [
          IconButton(
            tooltip: context.t('app.log.copy'),
            icon: const Icon(Icons.copy),
            onPressed: () => _copy(context),
          ),
          IconButton(
            tooltip: context.t('app.log.clear'),
            icon: const Icon(Icons.delete_outline),
            onPressed: log.clear,
          ),
        ],
      ),
      body: ListenableBuilder(
        listenable: log,
        builder: (context, _) {
          final entries = log.entries.reversed.toList();
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                child: Text(
                  context.t('app.log.info'),
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
              const Divider(height: 1),
              Expanded(
                child: entries.isEmpty
                    ? Center(child: Text(context.t('app.log.empty')))
                    : ListView.separated(
                        itemCount: entries.length,
                        separatorBuilder: (_, _) => const Divider(height: 1),
                        itemBuilder: (context, index) =>
                            _entry(context, entries[index]),
                      ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _entry(BuildContext context, LogEntry entry) {
    final scheme = Theme.of(context).colorScheme;
    final color = switch (entry.level) {
      LogLevel.error => scheme.error,
      LogLevel.warn => Colors.orange.shade700,
      LogLevel.info => scheme.onSurface,
      LogLevel.debug => scheme.onSurfaceVariant,
    };
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: SelectableText.rich(
        TextSpan(
          style: TextStyle(fontFamily: 'monospace', fontSize: 12, color: color),
          children: [
            TextSpan(
              text:
                  '${_time.format(entry.time)} ${entry.level.name.toUpperCase()} [${entry.tag}]\n',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            TextSpan(text: entry.message),
          ],
        ),
      ),
    );
  }
}
