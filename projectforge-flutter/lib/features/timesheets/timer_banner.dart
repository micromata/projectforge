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

import 'dart:async';

import 'package:flutter/material.dart';

import '../../core/i18n/translations.dart';
import 'time_utils.dart';
import 'timesheet.dart';

/// The running timer under the week strip: elapsed time, the task if already chosen, stop and discard.
class TimerBanner extends StatefulWidget {
  const TimerBanner({
    super.key,
    required this.timer,
    required this.onStop,
    required this.onDiscard,
  });

  final RunningTimer timer;
  final VoidCallback onStop;
  final VoidCallback onDiscard;

  @override
  State<TimerBanner> createState() => _TimerBannerState();
}

class _TimerBannerState extends State<TimerBanner> {
  late final Timer _ticker;

  @override
  void initState() {
    super.initState();
    _ticker = Timer.periodic(
      const Duration(seconds: 1),
      (_) => setState(() {}),
    );
  }

  @override
  void dispose() {
    _ticker.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = theme.colorScheme.onTertiaryContainer;
    final elapsed = DateTime.now().difference(widget.timer.start);
    final task = widget.timer.what?.task;
    final since = formatTime(widget.timer.start);
    return Material(
      color: theme.colorScheme.tertiaryContainer,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 6, 4, 6),
        child: Row(
          children: [
            Icon(Icons.timer_outlined, color: color),
            const SizedBox(width: 12),
            Text(
              '${formatDuration(elapsed)}:${two(elapsed.inSeconds % 60)}',
              style: theme.textTheme.titleMedium?.copyWith(
                fontFeatures: const [FontFeature.tabularFigures()],
                color: color,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                task == null
                    ? context.t('app.timesheet.timer.sinceNoTask', [since])
                    : context.t('app.timesheet.timer.since', [
                        since,
                        task.shortPath,
                      ]),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(color: color),
              ),
            ),
            IconButton(
              tooltip: context.t('app.timesheet.timer.discard'),
              icon: const Icon(Icons.delete_outline),
              onPressed: widget.onDiscard,
            ),
            FilledButton.tonalIcon(
              icon: const Icon(Icons.stop),
              label: Text(context.t('app.timesheet.timer.stop')),
              onPressed: widget.onStop,
            ),
          ],
        ),
      ),
    );
  }
}
