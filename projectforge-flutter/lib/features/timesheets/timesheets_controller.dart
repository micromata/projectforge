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

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/auth_controller.dart';
import 'timesheet.dart';
import 'timesheets_repository.dart';

final timesheetsRepositoryProvider = Provider<TimesheetsRepository?>((ref) {
  final api = ref.watch(apiClientProvider);
  final session = ref.watch(authControllerProvider).session;
  return api == null || session == null
      ? null
      : TimesheetsRepository(api, ref.watch(jsonCacheProvider), session.userId);
});

/// The bookings of the week starting on the given Monday.
final timesheetWeekProvider = FutureProvider.family<TimesheetWeek, DateTime>(
  (ref, monday) => ref.watch(timesheetsRepositoryProvider)!.week(monday),
);

/// What the user booked last, for the booking form (loaded again whenever it is opened).
final recentBookingsProvider = FutureProvider.autoDispose<List<BookingWhat>>(
  (ref) => ref.watch(timesheetsRepositoryProvider)!.recent(),
);

final timesheetTemplatesProvider =
    FutureProvider.autoDispose<List<TimesheetTemplate>>(
      (ref) => ref.watch(timesheetsRepositoryProvider)!.templates(),
    );

/// The tasks picked last, for the task search.
final recentTasksProvider = FutureProvider.autoDispose<List<TaskRef>>(
  (ref) => ref.watch(timesheetsRepositoryProvider)!.recentTasks(),
);

/// The bookable tasks matching a search term, below the task [parentId] only if given.
final taskSearchProvider = FutureProvider.autoDispose
    .family<List<TaskRef>, ({String term, int? parentId})>(
      (ref, search) => ref
          .watch(timesheetsRepositoryProvider)!
          .searchTasks(search.term, parentId: search.parentId),
    );

/// One level of the task tree, the top level for null.
final taskChildrenProvider = FutureProvider.autoDispose
    .family<List<TaskChild>, int?>(
      (ref, parentId) =>
          ref.watch(timesheetsRepositoryProvider)!.taskChildren(parentId),
    );

/// The cost units a booking on the task may charge.
final taskInfoProvider = FutureProvider.autoDispose.family<TaskInfo, int>(
  (ref, id) => ref.watch(timesheetsRepositoryProvider)!.taskInfo(id),
);

/// The running timer, kept on the device so it survives closing the app.
final timerProvider = AsyncNotifierProvider<TimerController, RunningTimer?>(
  TimerController.new,
);

class TimerController extends AsyncNotifier<RunningTimer?> {
  @override
  Future<RunningTimer?> build() =>
      ref.watch(timesheetsRepositoryProvider)!.loadTimer();

  TimesheetsRepository get _repository =>
      ref.read(timesheetsRepositoryProvider)!;

  Future<void> start([BookingWhat? what]) async {
    final timer = RunningTimer(DateTime.now(), what);
    state = AsyncData(timer);
    await _repository.saveTimer(timer);
  }

  Future<void> stop() async {
    state = const AsyncData(null);
    await _repository.clearTimer();
  }
}
