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
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/i18n/translations.dart';
import '../../core/widgets/error_view.dart';
import 'timesheet.dart';
import 'timesheets_controller.dart';

/// Full screen search for the task to book on: the bookable tasks matching the typed words of the path, and without
/// input the ones used last. Pops the chosen [TaskRef].
class TaskPicker extends ConsumerStatefulWidget {
  const TaskPicker({super.key, this.recentFromBookings = const []});

  /// Tasks of the latest bookings, offered after the ones picked last.
  final List<TaskRef> recentFromBookings;

  @override
  ConsumerState<TaskPicker> createState() => _TaskPickerState();
}

class _TaskPickerState extends ConsumerState<TaskPicker> {
  var _term = '';
  Timer? _debounce;

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  void _changed(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () {
      if (mounted) setState(() => _term = value.trim());
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: TextField(
          autofocus: true,
          decoration: InputDecoration(
            hintText: context.t('app.timesheet.task.search'),
            border: InputBorder.none,
          ),
          onChanged: _changed,
        ),
      ),
      body: _term.isEmpty ? _recent(context) : _results(context),
    );
  }

  Widget _recent(BuildContext context) {
    final picked = ref.watch(recentTasksProvider).value ?? const <TaskRef>[];
    final tasks = {...picked, ...widget.recentFromBookings}.toList();
    if (tasks.isEmpty) {
      return Center(child: Text(context.t('app.timesheet.task.hint')));
    }
    return ListView(
      children: [
        _Header(context.t('timesheet.recent')),
        for (final task in tasks) _tile(context, task, const []),
      ],
    );
  }

  Widget _results(BuildContext context) {
    final words = _term.toLowerCase().split(RegExp(r'\s+'));
    return ref
        .watch(taskSearchProvider(_term))
        .when(
          data: (tasks) => tasks.isEmpty
              ? Center(child: Text(context.t('app.timesheet.task.none')))
              : ListView(
                  children: [
                    for (final task in tasks) _tile(context, task, words),
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: Text(
                        context.t('app.timesheet.task.onlyBookable'),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ],
                ),
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => ErrorView(
            message: context.errorText(e),
            onRetry: () => ref.invalidate(taskSearchProvider(_term)),
          ),
        );
  }

  Widget _tile(BuildContext context, TaskRef task, List<String> words) {
    final theme = Theme.of(context);
    return ListTile(
      leading: const Icon(Icons.folder_outlined),
      title: Text(task.title),
      subtitle: task.parents.isEmpty
          ? null
          : Text.rich(_highlight(task.parents.join(' › '), words, theme)),
      onTap: () => Navigator.pop(context, task),
    );
  }

  TextSpan _highlight(String text, List<String> words, ThemeData theme) {
    final lower = text.toLowerCase();
    final marked = List.filled(text.length, false);
    for (final word in words.where((it) => it.isNotEmpty)) {
      var i = lower.indexOf(word);
      while (i >= 0) {
        marked.fillRange(i, i + word.length, true);
        i = lower.indexOf(word, i + word.length);
      }
    }
    final spans = <TextSpan>[];
    var start = 0;
    for (var i = 1; i <= text.length; i++) {
      if (i == text.length || marked[i] != marked[start]) {
        spans.add(
          TextSpan(
            text: text.substring(start, i),
            style: marked[start]
                ? TextStyle(
                    fontWeight: FontWeight.bold,
                    color: theme.colorScheme.primary,
                  )
                : null,
          ),
        );
        start = i;
      }
    }
    return TextSpan(children: spans);
  }
}

class _Header extends StatelessWidget {
  const _Header(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
    child: Text(
      text,
      style: Theme.of(context).textTheme.labelLarge
          ?.copyWith(color: Theme.of(context).colorScheme.primary),
    ),
  );
}
