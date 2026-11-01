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

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/cache/json_cache.dart';
import 'time_utils.dart';
import 'timesheet.dart';

/// The time sheets of the user (TimesheetEntityRest `/rs/timesheet`) and the tasks to book on (TaskServicesRest
/// `/rs/task`). Saving needs the server: its checks (overlap, closed tasks, cost units, protected periods) can't
/// be repeated on the device. Only the weeks seen last are kept for reading without network.
class TimesheetsRepository {
  TimesheetsRepository(this._api, this._cache, this.userId);

  static const _path = '/rs/timesheet';
  static const _taskPath = '/rs/task';
  static const _timerName = 'timesheetTimer';

  final ApiClient _api;
  final JsonCache _cache;
  final int userId;

  static String _weekName(DateTime monday) =>
      'timesheetWeek_${monday.year}${two(monday.month)}${two(monday.day)}';

  /// The bookings of the week starting on [monday], with the tags. Without a connection the week as it was loaded last
  /// (marked as such), if there is one.
  Future<TimesheetWeek> week(DateTime monday) async {
    try {
      final json = await _api.get<Map<String, dynamic>>(
        '$_path/appList',
        query: {
          'from': monday.millisecondsSinceEpoch,
          'to': addDays(monday, 7).millisecondsSinceEpoch,
        },
      );
      await _cache.write(_weekName(monday), json);
      return TimesheetWeek.fromJson(monday, json);
    } on ApiException catch (e) {
      final cached = e.isOffline ? await _cache.read(_weekName(monday)) : null;
      if (cached == null) rethrow;
      return TimesheetWeek.fromJson(monday, cached, fromCache: true);
    }
  }

  /// The whole booking as the server has it (the list rows are leaner).
  Future<Map<String, dynamic>> load(int id) =>
      _api.get<Map<String, dynamic>>('$_path/$id');

  /// Books [draft] as a new sheet, or as [id] if given. Throws the [ApiException] with the validation errors of the
  /// server (field `task`, `kost2`, or the period) if it refuses.
  Future<void> save(BookingDraft draft, {int? id}) async {
    // An edit starts from the whole sheet, so what this app doesn't show (AI time savings, …) isn't lost.
    final data = id == null ? <String, dynamic>{} : await load(id);
    data['task'] = {'id': draft.task.id};
    data['kost2'] = draft.kost2 == null ? null : {'id': draft.kost2!.id};
    data['startTime'] = draft.start.toUtc().toIso8601String();
    data['stopTime'] = draft.stop.toUtc().toIso8601String();
    data['description'] = draft.description;
    data['location'] = draft.location;
    data['reference'] = draft.reference;
    data['tag'] = draft.tag;
    if (id == null) data['user'] = {'id': userId};
    await _api.put<Object?>(
      '$_path/${id == null ? 'insert' : 'update'}',
      data: {'data': data},
    );
  }

  /// Marks the booking as deleted. Returns it as it was, for [undelete].
  Future<Map<String, dynamic>> delete(int id) async {
    final data = await load(id);
    await _api.delete<Object?>('$_path/markAsDeleted', data: {'data': data});
    return data;
  }

  Future<void> undelete(Map<String, dynamic> data) =>
      _api.put<Object?>('$_path/undelete', data: {'data': data});

  /// The bookings last made, newest first, without the same twice (`recentList`: what was booked, not when).
  Future<List<BookingWhat>> recent() async {
    final json = await _api.get<Map<String, dynamic>>('$_path/recentList');
    final seen = <String>{};
    return [
      for (final it in json['timesheets'] as List<dynamic>? ?? const [])
        if (BookingWhat.fromTimesheetJson(it as Map<String, dynamic>)
            case final what when what.task != null && seen.add(what.key))
          what,
    ];
  }

  Future<List<TimesheetTemplate>> templates() async {
    final list = await _api.get<List<dynamic>>('$_path/favorites/list');
    return [
      for (final it in list)
        TimesheetTemplate.fromJson(it as Map<String, dynamic>),
    ];
  }

  Future<void> createTemplate(String name, BookingWhat what) =>
      _api.post<Object?>(
        '$_path/favorites/create',
        data: {
          'name': name,
          'timesheet': {
            'task': {'id': what.task!.id},
            'kost2': what.kost2 == null ? null : {'id': what.kost2!.id},
            'description': what.description,
            'location': what.location,
            'reference': what.reference,
            'tag': what.tag,
          },
        },
      );

  Future<void> deleteTemplate(int id) =>
      _api.post<Object?>('$_path/favorites/delete', query: {'id': id});

  /// What a template books: its task (with the path) and cost unit.
  Future<BookingWhat> whatOf(TimesheetTemplate template) async {
    final taskId = template.taskId;
    final info = taskId == null ? null : await taskInfo(taskId);
    return BookingWhat(
      task: info?.task,
      kost2: info?.kost2List
          .where((it) => it.id == template.kost2Id)
          .firstOrNull,
      description: template.description,
      location: template.location,
      reference: template.reference,
      tag: template.tag,
    );
  }

  /// The bookable tasks matching [search] (words of the path), the ones of the user's booking grants first.
  Future<List<TaskRef>> searchTasks(String search) async {
    final list = await _api.get<List<dynamic>>(
      '$_taskPath/tree/autosearch',
      query: {'search': search, 'maxResults': 40, 'onlyBookable': true},
    );
    return [
      for (final it in list)
        TaskRef.fromDisplayObject(it as Map<String, dynamic>),
    ];
  }

  /// The tasks picked last, newest first.
  Future<List<TaskRef>> recentTasks() async {
    final list = await _api.get<List<dynamic>>('$_taskPath/recent/list');
    return [
      for (final it in list)
        TaskRef.fromDisplayObject(it as Map<String, dynamic>),
    ];
  }

  Future<TaskInfo> taskInfo(int id) async => TaskInfo.fromJson(
    await _api.get<Map<String, dynamic>>('$_taskPath/info/$id'),
  );

  /// The running timer, null if none.
  Future<RunningTimer?> loadTimer() async {
    final json = await _cache.read(_timerName);
    return json == null ? null : RunningTimer.fromJson(json);
  }

  Future<void> saveTimer(RunningTimer timer) =>
      _cache.write(_timerName, timer.toJson());

  Future<void> clearTimer() => _cache.delete(_timerName);
}
