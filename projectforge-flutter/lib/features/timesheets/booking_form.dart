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

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/i18n/translations.dart';
import 'task_picker.dart';
import 'time_utils.dart';
import 'timesheet.dart';
import 'timesheets_controller.dart';

/// What the form starts with: an existing booking ([id]), a gap, a duplicate, a stopped timer or a new one.
class BookingStart {
  const BookingStart({
    this.id,
    this.what,
    required this.start,
    required this.stop,
  });

  BookingStart.of(Timesheet sheet)
    : id = sheet.id,
      what = sheet.what,
      start = sheet.start,
      stop = sheet.stop;

  final int? id;
  final BookingWhat? what;
  final DateTime start;
  final DateTime stop;
}

/// The booking form: *what* first (recent entries, templates, task, cost unit), then *when* in one row, then the
/// text. A bottom sheet on a phone, a side panel on a tablet.
class BookingForm extends ConsumerStatefulWidget {
  const BookingForm({
    super.key,
    required this.initial,
    required this.tags,
    required this.onDone,
    this.onDelete,
  });

  final BookingStart initial;

  /// The tags the installation offers, empty where none is configured (the form then has no tag).
  final List<String> tags;

  /// Called with the saved booking, or null on cancel.
  final void Function(BookingDraft? saved) onDone;
  final VoidCallback? onDelete;

  @override
  ConsumerState<BookingForm> createState() => _BookingFormState();
}

class _BookingFormState extends ConsumerState<BookingForm> {
  TaskRef? _task;
  Kost2Ref? _kost2;

  /// The cost units the task offers, null while they are loaded.
  List<Kost2Ref>? _kost2Options;
  late DateTime _start;
  late DateTime _stop;
  String? _tag;
  final _description = TextEditingController();
  final _location = TextEditingController();
  final _reference = TextEditingController();
  var _errors = <String, String>{};
  var _saving = false;

  bool get _isEdit => widget.initial.id != null;

  @override
  void initState() {
    super.initState();
    _start = widget.initial.start;
    _stop = widget.initial.stop;
    final what = widget.initial.what;
    if (what != null) _apply(what);
  }

  @override
  void dispose() {
    _description.dispose();
    _location.dispose();
    _reference.dispose();
    super.dispose();
  }

  /// Fills the *what*, never the times (as the recent entries and templates of next do).
  void _apply(BookingWhat what) {
    _description.text = what.description;
    _location.text = what.location;
    _reference.text = what.reference;
    _tag = what.tag;
    _errors = {};
    final task = what.task;
    if (task == null) {
      _task = null;
      _kost2 = null;
      _kost2Options = null;
    } else {
      _setTask(task, kost2: what.kost2);
    }
  }

  void _setTask(TaskRef task, {Kost2Ref? kost2}) {
    _task = task;
    _kost2 = kost2;
    _kost2Options = null;
    _errors.remove('task');
    _errors.remove('kost2');
    _loadKost2(task);
  }

  Future<void> _loadKost2(TaskRef task) async {
    try {
      final info = await ref.read(taskInfoProvider(task.id).future);
      if (!mounted || _task != task) return;
      setState(() {
        _kost2Options = info.kost2List;
        // The only unit is chosen, one the task doesn't offer (any more) is dropped.
        if (info.kost2List.length == 1) {
          _kost2 = info.kost2List.first;
        } else if (_kost2 != null && !info.kost2List.contains(_kost2)) {
          _kost2 = null;
        }
      });
    } on Object {
      // The server reports a missing cost unit when saving.
      if (mounted && _task == task) setState(() => _kost2Options = const []);
    }
  }

  Duration get _duration => _stop.difference(_start);

  void _setStart(DateTime start) {
    final duration = _duration;
    setState(() {
      _start = start;
      _stop = start.add(duration);
      _errors.remove('time');
    });
  }

  /// A stop at or before the start counts as the next day (as next's time sheet form does).
  void _setStopTime(int hour, int minute) {
    var stop = DateTime(_start.year, _start.month, _start.day, hour, minute);
    if (!stop.isAfter(_start)) {
      stop = DateTime(stop.year, stop.month, stop.day + 1, hour, minute);
    }
    setState(() {
      _stop = stop;
      _errors.remove('time');
    });
  }

  Future<void> _save() async {
    final task = _task;
    if (task == null) {
      setState(
        () => _errors = {'task': context.t('app.timesheet.task.required')},
      );
      return;
    }
    final draft = BookingDraft(
      task: task,
      kost2: _kost2,
      start: _start,
      stop: _stop,
      description: _description.text.trim(),
      location: _location.text.trim(),
      reference: _reference.text.trim(),
      tag: _tag,
    );
    setState(() {
      _saving = true;
      _errors = {};
    });
    try {
      await ref
          .read(timesheetsRepositoryProvider)!
          .save(draft, id: widget.initial.id);
      ref.invalidate(timesheetWeekProvider);
      ref.invalidate(recentBookingsProvider);
      widget.onDone(draft);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _errors = _errorsOf(e);
      });
    }
  }

  /// The refusals of the server by the field they are about; whatever isn't about the task or the cost unit is
  /// about the period (overlap, protected period, duration).
  Map<String, String> _errorsOf(ApiException e) {
    if (e.validationErrors.isEmpty) return {'general': context.errorText(e)};
    final errors = <String, String>{};
    for (final it in e.validationErrors) {
      final field = it.fieldId == 'task' || it.fieldId == 'kost2'
          ? it.fieldId!
          : 'time';
      errors[field] = [?errors[field], ?it.message].join('\n');
    }
    return errors;
  }

  Future<void> _saveAsTemplate() async {
    final task = _task;
    if (task == null) {
      setState(
        () => _errors = {'task': context.t('app.timesheet.task.required')},
      );
      return;
    }
    final controller = TextEditingController(text: task.title);
    final name = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(context.t('app.timesheet.template.save')),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: InputDecoration(labelText: context.t('name')),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(context.t('cancel')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: Text(context.t('save')),
          ),
        ],
      ),
    );
    controller.dispose();
    if (name == null || name.isEmpty || !mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    final saved = context.t('app.timesheet.template.saved', [name]);
    try {
      await ref
          .read(timesheetsRepositoryProvider)!
          .createTemplate(
            name,
            BookingWhat(
              task: task,
              kost2: _kost2,
              description: _description.text.trim(),
              location: _location.text.trim(),
              reference: _reference.text.trim(),
              tag: _tag,
            ),
          );
      ref.invalidate(timesheetTemplatesProvider);
      messenger.showSnackBar(SnackBar(content: Text(saved)));
    } on ApiException catch (e) {
      if (mounted) setState(() => _errors = {'general': context.errorText(e)});
    }
  }

  Future<void> _applyTemplate(TimesheetTemplate template) async {
    try {
      final what = await ref
          .read(timesheetsRepositoryProvider)!
          .whatOf(template);
      if (mounted) setState(() => _apply(what));
    } on ApiException catch (e) {
      if (mounted) setState(() => _errors = {'general': context.errorText(e)});
    }
  }

  /// The templates in a sheet: one is applied by a tap, deleted by a swipe.
  Future<void> _showTemplates() async {
    final template = await showModalBottomSheet<TimesheetTemplate>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      useSafeArea: true,
      builder: (_) => _TemplateSheet(
        onDelete: _deleteTemplate,
        onSaveCurrent: _saveAsTemplate,
      ),
    );
    if (template != null) await _applyTemplate(template);
  }

  /// Whether the template was deleted.
  Future<bool> _deleteTemplate(TimesheetTemplate template) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        content: Text(
          context.t('app.timesheet.template.delete.question', [template.name]),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(context.t('cancel')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(context.t('delete')),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return false;
    try {
      await ref.read(timesheetsRepositoryProvider)!.deleteTemplate(template.id);
      ref.invalidate(timesheetTemplatesProvider);
      return true;
    } on ApiException catch (e) {
      if (mounted) setState(() => _errors = {'general': context.errorText(e)});
      return false;
    }
  }

  Future<void> _pickTask(List<BookingWhat> recent) async {
    final task = await TaskPicker.open(
      context,
      recentFromBookings: [
        for (final it in recent)
          if (it.task != null) it.task!,
      ],
      current: _task,
    );
    if (task != null) setState(() => _setTask(task));
  }

  Future<void> _pickTime({required bool start}) async {
    var picked = start ? _start : _stop;
    final use24h = MediaQuery.alwaysUse24HourFormatOf(context);
    final ok = await showModalBottomSheet<bool>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                const SizedBox(width: 16),
                Text(
                  context.t(start ? 'app.timesheet.from' : 'app.timesheet.to'),
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const Spacer(),
                TextButton(
                  onPressed: () => Navigator.pop(context, true),
                  child: Text(context.t('app.timesheet.done')),
                ),
              ],
            ),
            SizedBox(
              height: 216,
              child: CupertinoDatePicker(
                mode: CupertinoDatePickerMode.time,
                use24hFormat: use24h,
                minuteInterval: bookingStepMinutes,
                initialDateTime: floor5(picked),
                onDateTimeChanged: (t) => picked = t,
              ),
            ),
          ],
        ),
      ),
    );
    if (ok != true) return;
    if (start) {
      _setStart(
        DateTime(
          _start.year,
          _start.month,
          _start.day,
          picked.hour,
          picked.minute,
        ),
      );
    } else {
      _setStopTime(picked.hour, picked.minute);
    }
  }

  Future<void> _pickDate() async {
    final date = await showDatePicker(
      context: context,
      initialDate: _start,
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (date == null) return;
    setState(() {
      _start = DateTime(
        date.year,
        date.month,
        date.day,
        _start.hour,
        _start.minute,
      );
      _stop = _start.add(_duration);
      _errors.remove('time');
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      children: [
        _header(context, theme),
        const Divider(height: 1),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.only(bottom: 24),
            children: [
              ..._whatSection(context, theme),
              ..._whenSection(context, theme),
              ..._textSection(context, theme),
            ],
          ),
        ),
        _saveBar(context, theme),
      ],
    );
  }

  Widget _header(BuildContext context, ThemeData theme) => Padding(
    padding: const EdgeInsets.all(4),
    child: Row(
      children: [
        IconButton(
          icon: const Icon(Icons.close),
          tooltip: context.t('cancel'),
          onPressed: () => widget.onDone(null),
        ),
        Expanded(
          child: Text(
            _isEdit
                ? context.t('timesheet.title.edit')
                : context.t('timesheet.title.add'),
            style: theme.textTheme.titleLarge,
          ),
        ),
        PopupMenuButton<String>(
          onSelected: (value) {
            if (value == 'template') _saveAsTemplate();
            if (value == 'delete') widget.onDelete?.call();
          },
          itemBuilder: (context) => [
            PopupMenuItem(
              value: 'template',
              child: ListTile(
                leading: const Icon(Icons.bookmark_add_outlined),
                title: Text(context.t('app.timesheet.template.save')),
              ),
            ),
            if (_isEdit && widget.onDelete != null)
              PopupMenuItem(
                value: 'delete',
                child: ListTile(
                  leading: const Icon(Icons.delete_outline),
                  title: Text(context.t('delete')),
                ),
              ),
          ],
        ),
      ],
    ),
  );

  List<Widget> _whatSection(BuildContext context, ThemeData theme) {
    final task = _task;
    final recent =
        ref.watch(recentBookingsProvider).value ?? const <BookingWhat>[];
    final options = _kost2Options;
    return [
      Row(
        children: [
          _Label(context.t('app.timesheet.what')),
          const Spacer(),
          Padding(
            padding: const EdgeInsets.only(right: 8, top: 12),
            child: TextButton.icon(
              icon: const Icon(Icons.bookmark_outline, size: 18),
              label: Text(context.t('timesheet.templates')),
              onPressed: _showTemplates,
            ),
          ),
        ],
      ),
      if (!_isEdit && recent.isNotEmpty) ...[
        _SubLabel(context.t('timesheet.recent')),
        SizedBox(
          height: 62,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: recent.length,
            separatorBuilder: (_, _) => const SizedBox(width: 8),
            itemBuilder: (context, i) => _RecentCard(
              what: recent[i],
              selected: _isSame(recent[i]),
              onTap: () => setState(() => _apply(recent[i])),
            ),
          ),
        ),
      ],
      const SizedBox(height: 12),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Card.outlined(
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(
              color: _errors.containsKey('task')
                  ? theme.colorScheme.error
                  : theme.colorScheme.outlineVariant,
            ),
          ),
          child: ListTile(
            leading: Icon(task == null ? Icons.search : Icons.folder_outlined),
            title: task == null
                ? Text(context.t('app.timesheet.task.choose'))
                : TaskLabel(task),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => _pickTask(recent),
          ),
        ),
      ),
      _ErrorText(_errors['task']),
      if (task != null && options == null)
        const Padding(
          padding: EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: LinearProgressIndicator(),
        ),
      if (task != null && options != null && options.length == 1)
        Padding(
          padding: const EdgeInsets.fromLTRB(32, 8, 16, 0),
          child: Text(
            '${context.t('fibu.kost2')}: ${options.first.label}',
            style: theme.textTheme.bodySmall,
          ),
        ),
      if (task != null && options != null && options.length > 1)
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: DropdownButtonFormField<Kost2Ref>(
            // A new task starts with its own choice (cleared or the only one, see _loadKost2).
            key: ValueKey(task.id),
            initialValue: _kost2,
            isExpanded: true,
            // Items as high as their two lines (not the fixed 48 that clips them), the menu at most half the screen
            // high and scrolled within.
            itemHeight: null,
            menuMaxHeight: MediaQuery.sizeOf(context).height / 2,
            decoration: InputDecoration(
              labelText: context.t('fibu.kost2'),
              border: const OutlineInputBorder(),
              enabledBorder: _errors.containsKey('kost2')
                  ? OutlineInputBorder(
                      borderSide: BorderSide(color: theme.colorScheme.error),
                    )
                  : null,
            ),
            // Closed, the field keeps one line; the menu shows the number and the text below it.
            selectedItemBuilder: (context) => [
              for (final it in options)
                Text(it.label, maxLines: 1, overflow: TextOverflow.ellipsis),
            ],
            items: [
              for (final it in options)
                DropdownMenuItem(
                  value: it,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        it.number,
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      if (it.text.isNotEmpty)
                        Text(
                          it.text,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: theme.textTheme.bodySmall,
                        ),
                    ],
                  ),
                ),
            ],
            onChanged: (it) => setState(() {
              _kost2 = it;
              _errors.remove('kost2');
            }),
          ),
        ),
      _ErrorText(_errors['kost2']),
    ];
  }

  bool _isSame(BookingWhat what) =>
      what.task == _task &&
      what.kost2 == _kost2 &&
      what.description == _description.text;

  List<Widget> _whenSection(BuildContext context, ThemeData theme) {
    final locale = TranslationsScope.of(context).locale;
    final nextDay = dayOf(_stop) != dayOf(_start);
    final now = ceil5(DateTime.now());
    final untilNow = dayOf(_start) == dayOf(now) && now.isAfter(_start);
    return [
      Row(
        children: [
          _Label(context.t('app.timesheet.when')),
          const Spacer(),
          Padding(
            padding: const EdgeInsets.only(right: 8, top: 12),
            child: TextButton.icon(
              icon: const Icon(Icons.calendar_today, size: 18),
              label: Text(dayShort(locale, _start)),
              onPressed: _pickDate,
            ),
          ),
        ],
      ),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: Row(
          children: [
            _TimeButton(
              label: context.t('app.timesheet.from'),
              time: _start,
              error: _errors.containsKey('time'),
              onTap: () => _pickTime(start: true),
            ),
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 8),
              child: Icon(Icons.arrow_forward),
            ),
            _TimeButton(
              label: context.t(
                nextDay ? 'app.timesheet.toNextDay' : 'app.timesheet.to',
              ),
              time: _stop,
              error: _errors.containsKey('time'),
              onTap: () => _pickTime(start: false),
            ),
            Expanded(
              child: Align(
                alignment: Alignment.centerRight,
                child: FittedBox(
                  child: Text(
                    '${formatDuration(_duration)} h',
                    style: theme.textTheme.headlineSmall?.copyWith(
                      color: theme.colorScheme.primary,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
      _ErrorText(_errors['time']),
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
        child: Wrap(
          spacing: 8,
          children: [
            for (final minutes in const [15, 30, 60])
              ActionChip(
                label: Text(minutes == 60 ? '+1 h' : '+$minutes min'),
                onPressed: () => setState(() {
                  _stop = _stop.add(Duration(minutes: minutes));
                  _errors.remove('time');
                }),
              ),
            if (untilNow)
              ActionChip(
                avatar: const Icon(Icons.schedule, size: 18),
                label: Text(context.t('app.timesheet.untilNow')),
                onPressed: () => _setStopTime(now.hour, now.minute),
              ),
          ],
        ),
      ),
    ];
  }

  List<Widget> _textSection(BuildContext context, ThemeData theme) {
    final tags = [
      ...widget.tags,
      if (_tag != null && !widget.tags.contains(_tag)) _tag!,
    ];
    final locations = {
      for (final it
          in ref.watch(recentBookingsProvider).value ?? const <BookingWhat>[])
        if (it.location.isNotEmpty) it.location,
    };
    final more = [
      if (_location.text.isNotEmpty) _location.text,
      if (_reference.text.isNotEmpty) _reference.text,
      ?_tag,
    ].join(' · ');
    return [
      _Label(context.t('app.timesheet.whatDone')),
      Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        child: TextField(
          controller: _description,
          minLines: 2,
          maxLines: 5,
          textCapitalization: TextCapitalization.sentences,
          decoration: InputDecoration(
            hintText: context.t('timesheet.description'),
            border: const OutlineInputBorder(),
          ),
        ),
      ),
      const SizedBox(height: 8),
      ExpansionTile(
        title: Text(context.t('app.timesheet.more')),
        subtitle: more.isEmpty ? null : Text(more),
        childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
        children: [
          TextField(
            controller: _location,
            decoration: InputDecoration(
              labelText: context.t('timesheet.location'),
            ),
            onChanged: (_) => setState(() {}),
          ),
          if (locations.isNotEmpty)
            Align(
              alignment: Alignment.centerLeft,
              child: Wrap(
                spacing: 6,
                children: [
                  for (final it in locations)
                    ActionChip(
                      label: Text(it),
                      visualDensity: VisualDensity.compact,
                      onPressed: () => setState(() => _location.text = it),
                    ),
                ],
              ),
            ),
          TextField(
            controller: _reference,
            decoration: InputDecoration(
              labelText: context.t('timesheet.reference'),
            ),
            onChanged: (_) => setState(() {}),
          ),
          if (tags.isNotEmpty) ...[
            const SizedBox(height: 12),
            Align(
              alignment: Alignment.centerLeft,
              child: Wrap(
                spacing: 8,
                children: [
                  for (final it in tags)
                    FilterChip(
                      label: Text(it),
                      selected: _tag == it,
                      onSelected: (on) => setState(() => _tag = on ? it : null),
                    ),
                ],
              ),
            ),
          ],
        ],
      ),
    ];
  }

  Widget _saveBar(BuildContext context, ThemeData theme) {
    final general = _errors['general'];
    return Material(
      elevation: 3,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (general != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Row(
                    children: [
                      Icon(
                        Icons.cloud_off,
                        size: 18,
                        color: theme.colorScheme.error,
                      ),
                      const SizedBox(width: 8),
                      Expanded(child: Text(general)),
                    ],
                  ),
                ),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: FilledButton(
                  onPressed: _saving ? null : _save,
                  child: _saving
                      ? const SizedBox.square(
                          dimension: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Text(
                          _isEdit
                              ? context.t('save')
                              : context.t('app.timesheet.bookWithDuration', [
                                  formatDuration(_duration),
                                ]),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Label extends StatelessWidget {
  const _Label(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 20, 16, 8),
    child: Text(text, style: Theme.of(context).textTheme.titleMedium),
  );
}

class _SubLabel extends StatelessWidget {
  const _SubLabel(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(16, 4, 16, 6),
    child: Text(
      text,
      style: Theme.of(context).textTheme.labelMedium
          ?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
    ),
  );
}

/// The templates of the user: a tap pops the chosen one, a swipe deletes, the last row saves the current entry.
class _TemplateSheet extends ConsumerWidget {
  const _TemplateSheet({required this.onDelete, required this.onSaveCurrent});

  final Future<bool> Function(TimesheetTemplate template) onDelete;
  final VoidCallback onSaveCurrent;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final templates = ref.watch(timesheetTemplatesProvider);
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.6,
      minChildSize: 0.3,
      builder: (context, controller) => ListView(
        controller: controller,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Text(
              context.t('timesheet.templates'),
              style: theme.textTheme.titleMedium,
            ),
          ),
          ...templates.when(
            data: (list) => [
              if (list.isEmpty)
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text(context.t('app.timesheet.template.none')),
                ),
              for (final it in list)
                Dismissible(
                  key: ValueKey(it.id),
                  direction: DismissDirection.endToStart,
                  background: Container(
                    color: theme.colorScheme.errorContainer,
                    alignment: Alignment.centerRight,
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Icon(
                      Icons.delete_outline,
                      color: theme.colorScheme.onErrorContainer,
                    ),
                  ),
                  confirmDismiss: (_) => onDelete(it),
                  child: ListTile(
                    leading: const Icon(Icons.bookmark_outline),
                    title: Text(
                      it.name,
                      style: const TextStyle(fontWeight: FontWeight.w600),
                    ),
                    subtitle: _TemplateSubtitle(it),
                    onTap: () => Navigator.pop(context, it),
                  ),
                ),
            ],
            loading: () => const [
              Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              ),
            ],
            error: (e, _) => [
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text(context.errorText(e)),
              ),
            ],
          ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.bookmark_add_outlined),
            title: Text(context.t('app.timesheet.template.save')),
            onTap: () {
              Navigator.pop(context);
              onSaveCurrent();
            },
          ),
        ],
      ),
    );
  }
}

/// What a template books: the path of its task (loaded on demand) and its text.
class _TemplateSubtitle extends ConsumerWidget {
  const _TemplateSubtitle(this.template);

  final TimesheetTemplate template;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final taskId = template.taskId;
    final task = taskId == null
        ? null
        : ref.watch(taskInfoProvider(taskId)).value?.task;
    final lines = [
      if (task != null) task.fullPath,
      if (template.description.isNotEmpty) template.description,
    ];
    return lines.isEmpty
        ? const SizedBox.shrink()
        : Text(lines.join('\n'), maxLines: 3, overflow: TextOverflow.ellipsis);
  }
}

class _ErrorText extends StatelessWidget {
  const _ErrorText(this.text);

  final String? text;

  @override
  Widget build(BuildContext context) {
    final text = this.text;
    if (text == null) return const SizedBox.shrink();
    final color = Theme.of(context).colorScheme.error;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 6, 16, 0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.error_outline, size: 18, color: color),
          const SizedBox(width: 6),
          Expanded(
            child: Text(text, style: TextStyle(color: color)),
          ),
        ],
      ),
    );
  }
}

class _RecentCard extends StatelessWidget {
  const _RecentCard({
    required this.what,
    required this.selected,
    required this.onTap,
  });

  final BookingWhat what;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      color: selected
          ? theme.colorScheme.primaryContainer
          : theme.colorScheme.surfaceContainerHighest,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Container(
          width: 180,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                what.task?.shortPath ?? '',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.labelLarge,
              ),
              Text(
                what.description,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TimeButton extends StatelessWidget {
  const _TimeButton({
    required this.label,
    required this.time,
    required this.error,
    required this.onTap,
  });

  final String label;
  final DateTime time;
  final bool error;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OutlinedButton(
      style: OutlinedButton.styleFrom(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        side: BorderSide(
          color: error ? theme.colorScheme.error : theme.colorScheme.outline,
        ),
      ),
      onPressed: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: theme.textTheme.labelSmall),
          Text(formatTime(time), style: theme.textTheme.titleLarge),
        ],
      ),
    );
  }
}
