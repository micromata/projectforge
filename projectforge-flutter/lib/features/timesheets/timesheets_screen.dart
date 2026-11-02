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

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/i18n/translations.dart';
import '../../core/widgets/error_view.dart';
import 'booking_form.dart';
import 'time_utils.dart';
import 'timer_banner.dart';
import 'timesheet.dart';
import 'timesheets_controller.dart';

/// The time sheets: a week strip with the daily totals, the bookings of the selected day with the gaps between
/// them, "book" and the timer. Wide screens show the form as a side panel instead of a bottom sheet.
class TimesheetsScreen extends ConsumerStatefulWidget {
  const TimesheetsScreen({super.key, required this.active});

  /// Whether the tab is shown: the bookings are loaded again when it is opened.
  final bool active;

  @override
  ConsumerState<TimesheetsScreen> createState() => _TimesheetsScreenState();
}

class _TimesheetsScreenState extends ConsumerState<TimesheetsScreen> {
  static const _basePage = 10000;
  static const _wideLayout = 840.0;

  late DateTime _today = dayOf(DateTime.now());
  late DateTime _day = _today;
  late final _pages = PageController(initialPage: _basePage);

  BookingStart? _panel;
  var _panelFromTimer = false;
  var _panelKey = 0;
  var _wide = false;

  /// Deleted, but still in the list the server answered last.
  final _hidden = <int>{};

  @override
  void didUpdateWidget(TimesheetsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active && !oldWidget.active) {
      final today = dayOf(DateTime.now());
      if (today != _today) {
        // Opened on another day than the last time: back to today.
        _today = today;
        _select(today);
      }
      ref.invalidate(timesheetWeekProvider);
    }
  }

  @override
  void dispose() {
    _pages.dispose();
    super.dispose();
  }

  DateTime _dayOfPage(int page) => addDays(_today, page - _basePage);

  /// Rounded: between two midnights lie 23 or 25 hours when the daylight saving time changes.
  int _pageOfDay(DateTime day) =>
      _basePage + (dayOf(day).difference(_today).inHours / 24).round();

  void _select(DateTime day) {
    final page = _pageOfDay(day);
    final current = _pages.hasClients
        ? (_pages.page?.round() ?? _basePage)
        : _basePage;
    if (!_pages.hasClients) return;
    if ((page - current).abs() == 1) {
      _pages.animateToPage(
        page,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    } else {
      _pages.jumpToPage(page);
    }
  }

  AsyncValue<TimesheetWeek> _week(DateTime day) =>
      ref.watch(timesheetWeekProvider(mondayOf(day)));

  List<String> get _tags =>
      ref.read(timesheetWeekProvider(mondayOf(_day))).value?.tags ?? const [];

  void _open(BookingStart start, {bool fromTimer = false}) {
    if (_wide) {
      setState(() {
        _panel = start;
        _panelFromTimer = fromTimer;
        _panelKey++;
      });
      return;
    }
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (sheet) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(sheet).bottom),
        child: FractionallySizedBox(
          heightFactor: 0.95,
          child: BookingForm(
            initial: start,
            tags: _tags,
            onDone: (saved) {
              Navigator.pop(sheet);
              _done(saved, fromTimer: fromTimer);
            },
            onDelete: start.id == null
                ? null
                : () {
                    Navigator.pop(sheet);
                    _deleteById(start.id!);
                  },
          ),
        ),
      ),
    );
  }

  void _done(BookingDraft? saved, {required bool fromTimer}) {
    if (_panel != null) setState(() => _panel = null);
    if (saved == null) return;
    if (fromTimer) ref.read(timerProvider.notifier).stop();
    if (dayOf(saved.start) != _day) _select(saved.start);
    ScaffoldMessenger.of(context)
      ..clearSnackBars()
      ..showSnackBar(
        SnackBar(
          content: Text(
            context.t('app.timesheet.booked', [
              '${formatTime(saved.start)}–${formatTime(saved.stop)}',
              formatDuration(saved.stop.difference(saved.start)),
              saved.task.shortPath,
            ]),
          ),
        ),
      );
  }

  void _deleteById(int id) {
    final sheet = ref
        .read(timesheetWeekProvider(mondayOf(_day)))
        .value
        ?.sheets
        .where((it) => it.id == id)
        .firstOrNull;
    if (sheet != null) _delete(sheet);
  }

  Future<void> _delete(Timesheet sheet) async {
    final messenger = ScaffoldMessenger.of(context);
    final repository = ref.read(timesheetsRepositoryProvider)!;
    final deleted = context.t('app.timesheet.deleted', [
      '${formatTime(sheet.start)}–${formatTime(sheet.stop)}',
      sheet.what.task?.shortPath ?? '',
    ]);
    final undo = context.t('app.timesheet.undo');
    setState(() {
      _hidden.add(sheet.id);
      if (_panel?.id == sheet.id) _panel = null;
    });
    try {
      final data = await repository.delete(sheet.id);
      ref.invalidate(timesheetWeekProvider);
      messenger
        ..clearSnackBars()
        ..showSnackBar(
          SnackBar(
            content: Text(deleted),
            action: SnackBarAction(
              label: undo,
              onPressed: () async {
                try {
                  await repository.undelete(data);
                } on ApiException catch (e) {
                  messenger.showSnackBar(SnackBar(content: Text(e.message)));
                } finally {
                  if (mounted) {
                    setState(() => _hidden.remove(sheet.id));
                    ref.invalidate(timesheetWeekProvider);
                  }
                }
              },
            ),
          ),
        );
    } on ApiException catch (e) {
      if (mounted) setState(() => _hidden.remove(sheet.id));
      messenger.showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  void _new() {
    final start =
        _week(_day).value?.nextStartOf(_day) ??
        dayOf(_day).add(const Duration(hours: defaultFirstHour));
    _open(
      BookingStart(start: start, stop: start.add(const Duration(hours: 1))),
    );
  }

  void _duplicate(Timesheet sheet) {
    final start = _week(_day).value?.nextStartOf(_day) ?? sheet.stop;
    _open(
      BookingStart(
        what: sheet.what,
        start: start,
        stop: start.add(sheet.duration),
      ),
    );
  }

  void _stopTimer(RunningTimer timer) {
    final start = floor5(timer.start);
    var stop = ceil5(DateTime.now());
    if (!stop.isAfter(start)) {
      stop = start.add(const Duration(minutes: bookingStepMinutes));
    }
    _open(
      BookingStart(what: timer.what, start: start, stop: stop),
      fromTimer: true,
    );
  }

  void _startTimer([BookingWhat? what]) {
    ref.read(timerProvider.notifier).start(what);
    ScaffoldMessenger.of(context)
      ..clearSnackBars()
      ..showSnackBar(
        SnackBar(content: Text(context.t('app.timesheet.timer.started'))),
      );
  }

  @override
  Widget build(BuildContext context) {
    final locale = TranslationsScope.of(context).locale;
    final week = _week(_day).value;
    final scheme = Theme.of(context).colorScheme;
    return Column(
      children: [
        Material(
          color: scheme.tertiaryContainer,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                Icon(Icons.construction, color: scheme.onTertiaryContainer),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    context.t('app.underConstruction'),
                    style: TextStyle(color: scheme.onTertiaryContainer),
                  ),
                ),
              ],
            ),
          ),
        ),
        Expanded(
          child: LayoutBuilder(
            builder: (context, constraints) {
              _wide = constraints.maxWidth >= _wideLayout;
              final main = _main(context, locale, week);
              return _wide
                  ? Row(
                      children: [
                        Expanded(child: main),
                        const VerticalDivider(width: 1),
                        SizedBox(width: 440, child: _sidePanel(context)),
                      ],
                    )
                  : main;
            },
          ),
        ),
      ],
    );
  }

  Widget _main(BuildContext context, String locale, TimesheetWeek? week) {
    final theme = Theme.of(context);
    final timer = ref.watch(timerProvider).value;
    return Stack(
      children: [
        Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 8, 0),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          context.t('app.timesheet.weekTitle', [
                            weekOfYear(_day),
                            monthYear(locale, _day),
                          ]),
                          style: theme.textTheme.titleMedium,
                        ),
                        if (week != null)
                          Text(
                            context.t('app.timesheet.weekTotal', [
                              formatDuration(week.total),
                            ]),
                            style: theme.textTheme.bodySmall,
                          ),
                      ],
                    ),
                  ),
                  if (_day != _today)
                    TextButton(
                      onPressed: () => _select(_today),
                      child: Text(context.t('calendar.today')),
                    ),
                ],
              ),
            ),
            _WeekStrip(
              locale: locale,
              week: week,
              monday: mondayOf(_day),
              day: _day,
              today: _today,
              onSelect: _select,
            ),
            const Divider(height: 1),
            if (timer != null)
              TimerBanner(
                timer: timer,
                onStop: () => _stopTimer(timer),
                onDiscard: ref.read(timerProvider.notifier).stop,
              ),
            Expanded(
              child: PageView.builder(
                controller: _pages,
                onPageChanged: (page) =>
                    setState(() => _day = _dayOfPage(page)),
                itemBuilder: (context, page) =>
                    _dayList(context, locale, _dayOfPage(page)),
              ),
            ),
          ],
        ),
        Positioned(
          right: 16,
          bottom: 16,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              if (timer == null)
                FloatingActionButton.small(
                  heroTag: 'timesheetTimer',
                  tooltip: context.t('app.timesheet.timer.start'),
                  onPressed: _startTimer,
                  child: const Icon(Icons.timer_outlined),
                ),
              const SizedBox(height: 12),
              FloatingActionButton.extended(
                heroTag: 'timesheetBook',
                onPressed: _new,
                icon: const Icon(Icons.add),
                label: Text(context.t('app.timesheet.book')),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _sidePanel(BuildContext context) {
    final panel = _panel;
    if (panel == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Text(
            context.t('app.timesheet.panel.hint'),
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyLarge,
          ),
        ),
      );
    }
    return BookingForm(
      key: ValueKey(_panelKey),
      initial: panel,
      tags: _tags,
      onDone: (saved) => _done(saved, fromTimer: _panelFromTimer),
      onDelete: panel.id == null ? null : () => _deleteById(panel.id!),
    );
  }

  Widget _dayList(BuildContext context, String locale, DateTime day) {
    final theme = Theme.of(context);
    final async = _week(day);
    final week = async.value;
    if (week == null) {
      return async.hasError
          ? ErrorView(
              message: context.errorText(async.error!),
              onRetry: () =>
                  ref.invalidate(timesheetWeekProvider(mondayOf(day))),
            )
          : const Center(child: CircularProgressIndicator());
    }
    final bookings = [
      for (final it in week.bookingsOf(day))
        if (!_hidden.contains(it.id)) it,
    ];
    final total = bookings.fold(Duration.zero, (sum, it) => sum + it.duration);
    final items = <Object>[...bookings, ...week.gapsOf(day, DateTime.now())]
      ..sort((a, b) => _startOf(a).compareTo(_startOf(b)));
    return RefreshIndicator(
      onRefresh: () async {
        try {
          final reloaded = ref.refresh(
            timesheetWeekProvider(mondayOf(day)).future,
          );
          await reloaded;
        } on Object {
          // The error is shown by the list.
        }
      },
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 160),
        children: [
          if (week.fromCache)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: Row(
                children: [
                  Icon(
                    Icons.cloud_off,
                    size: 18,
                    color: theme.colorScheme.outline,
                  ),
                  const SizedBox(width: 8),
                  Expanded(child: Text(context.t('app.timesheet.offline'))),
                ],
              ),
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    dayLong(locale, day),
                    style: theme.textTheme.titleMedium,
                  ),
                ),
                Text(
                  'Σ ${formatDuration(total)} h',
                  style: theme.textTheme.titleMedium,
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: (total.inMinutes / targetPerDay.inMinutes)
                    .clamp(0, 1)
                    .toDouble(),
                minHeight: 6,
              ),
            ),
          ),
          if (items.isEmpty) _empty(context, day, week),
          for (final item in items)
            if (item is Timesheet)
              _bookingCard(context, item)
            else
              _gapRow(context, item as Gap),
        ],
      ),
    );
  }

  static DateTime _startOf(Object item) =>
      item is Timesheet ? item.start : (item as Gap).start;

  Widget _empty(BuildContext context, DateTime day, TimesheetWeek week) {
    final weekend = day.weekday > DateTime.friday;
    return Padding(
      padding: const EdgeInsets.fromLTRB(32, 48, 32, 0),
      child: Column(
        children: [
          Icon(
            Icons.event_available,
            size: 48,
            color: Theme.of(context).colorScheme.outline,
          ),
          const SizedBox(height: 12),
          Text(
            context.t(
              weekend ? 'app.timesheet.emptyWeekend' : 'app.timesheet.empty',
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.tonalIcon(
            icon: const Icon(Icons.add),
            label: Text(
              context.t('app.timesheet.bookFrom', [
                formatTime(week.nextStartOf(day)),
              ]),
            ),
            onPressed: () {
              final start = week.nextStartOf(day);
              _open(
                BookingStart(
                  start: start,
                  stop: start.add(const Duration(hours: 1)),
                ),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _bookingCard(BuildContext context, Timesheet sheet) {
    final theme = Theme.of(context);
    final what = sheet.what;
    final selected = _wide && _panel?.id == sheet.id;
    final subdued = theme.textTheme.bodySmall?.copyWith(
      color: theme.colorScheme.onSurfaceVariant,
    );
    final extra = [
      if (what.location.isNotEmpty) what.location,
      ?what.tag,
    ].join(' · ');
    return Dismissible(
      key: ValueKey(sheet.id),
      background: _swipeBackground(
        Icons.copy,
        context.t('app.timesheet.duplicate'),
        theme.colorScheme.primaryContainer,
        Alignment.centerLeft,
      ),
      secondaryBackground: _swipeBackground(
        Icons.delete_outline,
        context.t('delete'),
        theme.colorScheme.errorContainer,
        Alignment.centerRight,
      ),
      confirmDismiss: (direction) async {
        if (direction == DismissDirection.startToEnd) {
          _duplicate(sheet);
          return false;
        }
        return true;
      },
      onDismissed: (_) => _delete(sheet),
      child: Card(
        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        color: selected ? theme.colorScheme.primaryContainer : null,
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          onTap: () => _open(BookingStart.of(sheet)),
          onLongPress: () => _actions(context, sheet),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                SizedBox(
                  width: 52,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        formatTime(sheet.start),
                        style: theme.textTheme.titleSmall,
                      ),
                      Text(formatTime(sheet.stop), style: subdued),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        what.task?.shortPath ?? '',
                        style: theme.textTheme.titleSmall,
                      ),
                      if (what.kost2 != null)
                        Text(what.kost2!.label, style: subdued),
                      if (what.description.isNotEmpty)
                        Text(
                          what.description,
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                      if (extra.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 2),
                          child: Text(extra, style: theme.textTheme.labelSmall),
                        ),
                      if (sheet.acceptancePending)
                        Padding(
                          padding: const EdgeInsets.only(top: 2),
                          child: Text(
                            context.t('app.timesheet.acceptancePending'),
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: theme.colorScheme.tertiary,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                Text(
                  formatDuration(sheet.duration),
                  style: theme.textTheme.titleMedium,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _swipeBackground(
    IconData icon,
    String label,
    Color color,
    Alignment alignment,
  ) => Container(
    margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
    padding: const EdgeInsets.symmetric(horizontal: 20),
    decoration: BoxDecoration(
      color: color,
      borderRadius: BorderRadius.circular(12),
    ),
    alignment: alignment,
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [Icon(icon), const SizedBox(width: 8), Text(label)],
    ),
  );

  Widget _gapRow(BuildContext context, Gap gap) {
    final theme = Theme.of(context);
    final color = theme.colorScheme.outline;
    return InkWell(
      onTap: () => _open(BookingStart(start: gap.start, stop: gap.stop)),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        child: Row(
          children: [
            SizedBox(width: 16, child: Divider(color: color)),
            const SizedBox(width: 8),
            Icon(
              Icons.add_circle_outline,
              size: 18,
              color: theme.colorScheme.primary,
            ),
            const SizedBox(width: 6),
            Flexible(
              flex: 8,
              child: Text(
                context.t('app.timesheet.gap', [
                  formatTime(gap.start),
                  formatTime(gap.stop),
                  formatDuration(gap.duration),
                ]),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: theme.colorScheme.primary,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(child: Divider(color: color)),
          ],
        ),
      ),
    );
  }

  void _actions(BuildContext context, Timesheet sheet) {
    showModalBottomSheet<void>(
      context: context,
      builder: (menu) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              title: Text(sheet.what.task?.shortPath ?? ''),
              subtitle: Text(
                '${formatTime(sheet.start)}–${formatTime(sheet.stop)} · ${sheet.what.description}',
              ),
            ),
            const Divider(height: 1),
            ListTile(
              leading: const Icon(Icons.edit_outlined),
              title: Text(context.t('app.timesheet.edit')),
              onTap: () {
                Navigator.pop(menu);
                _open(BookingStart.of(sheet));
              },
            ),
            ListTile(
              leading: const Icon(Icons.copy),
              title: Text(context.t('app.timesheet.duplicate.next')),
              onTap: () {
                Navigator.pop(menu);
                _duplicate(sheet);
              },
            ),
            ListTile(
              leading: const Icon(Icons.timer_outlined),
              title: Text(context.t('app.timesheet.continue')),
              enabled: ref.read(timerProvider).value == null,
              onTap: () {
                Navigator.pop(menu);
                _startTimer(sheet.what);
              },
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline),
              title: Text(context.t('delete')),
              onTap: () {
                Navigator.pop(menu);
                _delete(sheet);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _WeekStrip extends StatelessWidget {
  const _WeekStrip({
    required this.locale,
    required this.week,
    required this.monday,
    required this.day,
    required this.today,
    required this.onSelect,
  });

  final String locale;

  /// Null while the week is loaded.
  final TimesheetWeek? week;
  final DateTime monday;
  final DateTime day;
  final DateTime today;
  final ValueChanged<DateTime> onSelect;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onHorizontalDragEnd: (details) {
        final velocity = details.primaryVelocity ?? 0;
        if (velocity.abs() < 200) return;
        onSelect(addDays(day, velocity < 0 ? 7 : -7));
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
        child: Row(
          children: [
            IconButton(
              tooltip: context.t('app.timesheet.week.previous'),
              icon: const Icon(Icons.chevron_left),
              onPressed: () => onSelect(addDays(day, -7)),
            ),
            for (var i = 0; i < 7; i++)
              Expanded(child: _dayCell(context, addDays(monday, i))),
            IconButton(
              tooltip: context.t('app.timesheet.week.next'),
              icon: const Icon(Icons.chevron_right),
              onPressed: () => onSelect(addDays(day, 7)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _dayCell(BuildContext context, DateTime d) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final selected = d == day;
    final total = week?.totalOf(d) ?? Duration.zero;
    final weekend = d.weekday > DateTime.friday;
    final barColor = total >= targetPerDay
        ? scheme.primary
        : total > Duration.zero
        ? scheme.tertiary
        : scheme.outlineVariant;
    final dim = weekend && !selected ? scheme.onSurfaceVariant : null;
    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: () => onSelect(d),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 6),
        decoration: BoxDecoration(
          color: selected ? scheme.primaryContainer : null,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          children: [
            Text(
              weekdayShort(locale, d),
              style: theme.textTheme.labelSmall?.copyWith(color: dim),
            ),
            Text(
              '${d.day}',
              style: theme.textTheme.titleMedium?.copyWith(
                color: d == today ? scheme.primary : dim,
                fontWeight: d == today ? FontWeight.bold : null,
                decoration: d == today ? TextDecoration.underline : null,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              total == Duration.zero ? '–' : formatDuration(total),
              style: theme.textTheme.labelSmall?.copyWith(color: dim),
            ),
            const SizedBox(height: 4),
            Container(
              width: 24,
              height: 3,
              decoration: BoxDecoration(
                color: barColor,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
