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

/// Full screen search for the task to book on: the bookable tasks matching the typed words of the path, without
/// input the ones used last and the way into the task tree. Pops the chosen [TaskRef].
class TaskPicker extends ConsumerStatefulWidget {
  const TaskPicker({
    super.key,
    this.recentFromBookings = const [],
    this.current,
  });

  static const routeName = 'taskPicker';

  /// Pushes the picker, the result is the chosen task or null.
  static Future<TaskRef?> open(
    BuildContext context, {
    List<TaskRef> recentFromBookings = const [],
    TaskRef? current,
  }) => Navigator.of(context).push<TaskRef>(
    MaterialPageRoute(
      settings: const RouteSettings(name: routeName),
      builder: (_) =>
          TaskPicker(recentFromBookings: recentFromBookings, current: current),
    ),
  );

  /// Tasks of the latest bookings, offered after the ones picked last.
  final List<TaskRef> recentFromBookings;

  /// The task chosen so far: marked in the lists, and the tree opens at its level.
  final TaskRef? current;

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

  /// Closes the levels of the tree and the picker with [task].
  void _pick(TaskRef task) {
    final navigator = Navigator.of(context);
    navigator.popUntil((route) => route.settings.name == TaskPicker.routeName);
    navigator.pop(task);
  }

  static String _levelName(int? id) => 'taskLevel:${id ?? ''}';

  void _openLevel(List<_Crumb> path) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        settings: RouteSettings(name: _levelName(path.lastOrNull?.id)),
        builder: (_) => _TaskLevel(
          path: path,
          current: widget.current,
          onOpen: _openLevel,
          onJump: _jumpTo,
          onPick: _pick,
        ),
      ),
    );
  }

  /// Goes back up to the level of the task [id] (the top level for null).
  void _jumpTo(int? id) =>
      Navigator.of(context)
          .popUntil((route) => route.settings.name == _levelName(id));

  /// The top level and, with a task chosen so far, the levels down to its parent: one route each, so going back
  /// (the swipe on iOS) goes up the tree.
  Future<void> _openTree() async {
    var path = const <_Crumb>[];
    final current = widget.current;
    if (current != null) {
      try {
        final info = await ref.read(taskInfoProvider(current.id).future);
        if (info.pathIds.length == info.task.parents.length) {
          path = [
            for (var i = 0; i < info.pathIds.length; i++)
              _Crumb(info.pathIds[i], info.task.parents[i]),
          ];
        }
      } on Object {
        // Then from the top only.
      }
    }
    if (!mounted) return;
    _openLevel(const []);
    for (var i = 1; i <= path.length; i++) {
      _openLevel(path.sublist(0, i));
    }
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
    return ListView(
      children: [
        ListTile(
          leading: const Icon(Icons.account_tree_outlined),
          title: Text(context.t('app.timesheet.task.tree')),
          trailing: const Icon(Icons.chevron_right),
          onTap: _openTree,
        ),
        const Divider(height: 1),
        if (tasks.isEmpty)
          Padding(
            padding: const EdgeInsets.all(16),
            child: Text(
              context.t('app.timesheet.task.hint'),
              textAlign: TextAlign.center,
            ),
          )
        else ...[
          _Header(context.t('timesheet.recent')),
          for (final task in tasks) _tile(context, task, const []),
        ],
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

  Widget _tile(BuildContext context, TaskRef task, List<String> words) =>
      ListTile(
        leading: const Icon(Icons.folder_outlined),
        title: TaskLabel(task, words: words),
        selected: task == widget.current,
        onTap: () => _pick(task),
      );
}

/// A task of the path to a level of the tree.
class _Crumb {
  const _Crumb(this.id, this.title);

  final int id;
  final String title;
}

/// One level of the task tree: the children of the last task of [path] (the top level if empty), the path above them
/// to go back up.
class _TaskLevel extends ConsumerWidget {
  const _TaskLevel({
    required this.path,
    required this.current,
    required this.onOpen,
    required this.onJump,
    required this.onPick,
  });

  final List<_Crumb> path;
  final TaskRef? current;
  final void Function(List<_Crumb> path) onOpen;
  final void Function(int? id) onJump;
  final void Function(TaskRef task) onPick;

  TaskRef _taskOf(int id, String title) =>
      TaskRef(id: id, title: title, parents: [for (final it in path) it.title]);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final parent = path.lastOrNull;
    final children = taskChildrenProvider(parent?.id);
    // Whether the parent itself may be booked on: as the level above has it (still open beneath this one).
    final parentBookable =
        parent != null &&
        (ref
                .watch(
                  taskChildrenProvider(
                    path.length > 1 ? path[path.length - 2].id : null,
                  ),
                )
                .value
                ?.where((it) => it.id == parent.id)
                .firstOrNull
                ?.bookable ??
            false);
    return Scaffold(
      appBar: AppBar(
        title: Text(parent?.title ?? context.t('app.timesheet.task.tree')),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (path.isNotEmpty) _breadcrumbs(context, theme),
          Expanded(
            child: ref
                .watch(children)
                .when(
                  data: (list) => ListView(
                    children: [
                      if (parentBookable)
                        ListTile(
                          leading: const Icon(Icons.check_circle_outline),
                          title: Text(
                            context.t('app.timesheet.task.bookOn', [
                              parent.title,
                            ]),
                          ),
                          selected: current?.id == parent.id,
                          onTap: () => onPick(
                            TaskRef(
                              id: parent.id,
                              title: parent.title,
                              parents: [
                                for (final it in path.take(path.length - 1))
                                  it.title,
                              ],
                            ),
                          ),
                        ),
                      if (list.isEmpty && !parentBookable)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Text(
                            context.t('app.timesheet.task.treeEmpty'),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      for (final it in list)
                        ListTile(
                          leading: Icon(
                            it.hasChildren
                                ? Icons.folder_outlined
                                : Icons.description_outlined,
                          ),
                          title: Text(it.title),
                          trailing: it.hasChildren
                              ? const Icon(Icons.chevron_right)
                              : null,
                          enabled: it.hasChildren || it.bookable,
                          selected: current?.id == it.id,
                          onTap: () => it.hasChildren
                              ? onOpen([...path, _Crumb(it.id, it.title)])
                              : onPick(_taskOf(it.id, it.title)),
                        ),
                    ],
                  ),
                  loading: () =>
                      const Center(child: CircularProgressIndicator()),
                  error: (e, _) => ErrorView(
                    message: context.errorText(e),
                    onRetry: () => ref.invalidate(children),
                  ),
                ),
          ),
        ],
      ),
    );
  }

  /// The way back up: the top level and the tasks above this level.
  Widget _breadcrumbs(BuildContext context, ThemeData theme) => SizedBox(
    height: 44,
    child: ListView(
      scrollDirection: Axis.horizontal,
      reverse: true,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      children: [
        for (final it in [null, ...path.take(path.length - 1)].reversed) ...[
          // Reversed, so the levels nearest to this one are seen first: the button, then the separator before it.
          it == null
              ? IconButton(
                  icon: const Icon(Icons.home_outlined),
                  tooltip: context.t('app.timesheet.task.tree'),
                  onPressed: () => onJump(null),
                )
              : TextButton(
                  onPressed: () => onJump(it.id),
                  child: Text(it.title),
                ),
          if (it != null)
            Icon(
              Icons.chevron_right,
              size: 18,
              color: theme.colorScheme.onSurfaceVariant,
            ),
        ],
      ],
    ),
  );
}

/// The title of a task, the path above it in small letters (as the web shows it), [words] of a search marked.
class TaskLabel extends StatelessWidget {
  const TaskLabel(this.task, {super.key, this.words = const []});

  final TaskRef task;
  final List<String> words;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        if (task.parents.isNotEmpty)
          Text.rich(
            _highlight(task.parents.join(' › '), words, theme),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        Text.rich(
          _highlight(task.title, words, theme),
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ],
    );
  }
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
