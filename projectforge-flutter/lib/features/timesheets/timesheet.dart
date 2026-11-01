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

import 'time_utils.dart';

/// A task (structure element) as the app shows it: its title and the titles of the tasks above it.
class TaskRef {
  const TaskRef({
    required this.id,
    required this.title,
    this.parents = const [],
  });

  /// The task of a time sheet, a recent entry or `task/info` (Kotlin `rest.dto.Task`): `title`, and the ancestors
  /// as `path` (a list of tasks) or, on a list row, as one text.
  factory TaskRef.fromJson(Map<String, dynamic> json) {
    final path = json['path'];
    final parents = switch (path) {
      final List<dynamic> list => [
        for (final it in list)
          if (it is Map && it['title'] != null) '${it['title']}',
      ],
      final String text when text.isNotEmpty => text.split(_separator),
      _ => const <String>[],
    };
    final title =
        json['title'] as String? ??
        json['displayName'] as String? ??
        '#${json['id']}';
    return TaskRef(
      id: (json['id'] as num).toInt(),
      title: title,
      parents: parents,
    );
  }

  /// A hit of the task search, `{id, displayName}` with the whole path as `A | B | C`.
  factory TaskRef.fromDisplayObject(Map<String, dynamic> json) {
    final titles = (json['displayName'] as String? ?? '').split(_separator);
    return TaskRef(
      id: (json['id'] as num).toInt(),
      title: titles.last,
      parents: titles.sublist(0, titles.length - 1),
    );
  }

  static final _separator = RegExp(r'\s*(?:\||->)\s*');

  final int id;
  final String title;
  final List<String> parents;

  String get fullPath => [...parents, title].join(' › ');

  /// The title and its parent: enough to recognize the task in a row.
  String get shortPath => parents.isEmpty ? title : '${parents.last} › $title';

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'path': [
      for (final it in parents) {'title': it},
    ],
  };

  @override
  bool operator ==(Object other) => other is TaskRef && other.id == id;

  @override
  int get hashCode => id.hashCode;
}

/// A cost unit (Kost2) of a task.
class Kost2Ref {
  const Kost2Ref({required this.id, required this.label});

  /// A cost unit of `task/info` or of a time sheet (Kotlin `rest.dto.Kost2`).
  factory Kost2Ref.fromJson(Map<String, dynamic> json) {
    final number = json['formattedNumber'] as String?;
    final description = json['description'] as String?;
    final label = number != null
        ? (description == null || description.isEmpty
              ? number
              : '$number $description')
        : json['displayName'] as String? ?? '#${json['id']}';
    return Kost2Ref(id: (json['id'] as num).toInt(), label: label);
  }

  final int id;
  final String label;

  Map<String, dynamic> toJson() => {'id': id, 'displayName': label};

  @override
  bool operator ==(Object other) => other is Kost2Ref && other.id == id;

  @override
  int get hashCode => id.hashCode;
}

/// What was done, without the times: the part a recent entry, a template or a stopped timer fills in.
class BookingWhat {
  const BookingWhat({
    this.task,
    this.kost2,
    this.description = '',
    this.location = '',
    this.reference = '',
    this.tag,
  });

  /// From a time sheet or recent entry (Kotlin `rest.dto.Timesheet`).
  factory BookingWhat.fromTimesheetJson(Map<String, dynamic> json) {
    final task = json['task'];
    final kost2 = json['kost2'];
    return BookingWhat(
      task: task is Map<String, dynamic> ? TaskRef.fromJson(task) : null,
      kost2: kost2 is Map<String, dynamic> ? Kost2Ref.fromJson(kost2) : null,
      description: json['description'] as String? ?? '',
      location: json['location'] as String? ?? '',
      reference: json['reference'] as String? ?? '',
      tag: (json['tag'] as String?)?.isEmpty ?? true
          ? null
          : json['tag'] as String?,
    );
  }

  /// From the file of the running timer.
  factory BookingWhat.fromJson(Map<String, dynamic> json) =>
      BookingWhat.fromTimesheetJson(json);

  final TaskRef? task;
  final Kost2Ref? kost2;
  final String description;
  final String location;
  final String reference;
  final String? tag;

  Map<String, dynamic> toJson() => {
    'task': task?.toJson(),
    'kost2': kost2?.toJson(),
    'description': description,
    'location': location,
    'reference': reference,
    'tag': tag,
  };

  /// Two entries that book the same: shown once in the recent list.
  String get key =>
      '${task?.id}|${kost2?.id}|$description|$location|$reference|$tag';
}

/// A booking of the user (Kotlin `rest.dto.Timesheet`). Keeps the json of the server, so the fields this app doesn't
/// know (AI time savings, acceptance, …) survive an edit.
class Timesheet {
  Timesheet(this.json)
    : id = (json['id'] as num).toInt(),
      what = BookingWhat.fromTimesheetJson(json),
      start = DateTime.parse(json['startTime'] as String).toLocal(),
      stop = DateTime.parse(json['stopTime'] as String).toLocal();

  final Map<String, dynamic> json;
  final int id;
  final BookingWhat what;
  final DateTime start;
  final DateTime stop;

  Duration get duration => stop.difference(start);

  /// Booked by a proposed grant and not yet accepted by the project management.
  bool get acceptancePending => json['acceptance'] != null;
}

/// A free time between two bookings of a day.
class Gap {
  const Gap(this.start, this.stop);

  final DateTime start;
  final DateTime stop;

  Duration get duration => stop.difference(start);
}

/// The bookings of a week (Monday to Sunday) and the tags the installation offers.
class TimesheetWeek {
  TimesheetWeek(
    this.monday,
    List<Timesheet> sheets,
    this.tags, {
    this.fromCache = false,
  }) : sheets = List.unmodifiable(sheets);

  factory TimesheetWeek.fromJson(
    DateTime monday,
    Map<String, dynamic> json, {
    bool fromCache = false,
  }) => TimesheetWeek(
    monday,
    [
      for (final it in json['timesheets'] as List<dynamic>? ?? const [])
        Timesheet(it as Map<String, dynamic>),
    ],
    [for (final it in json['tags'] as List<dynamic>? ?? const []) '$it'],
    fromCache: fromCache,
  );

  final DateTime monday;
  final List<Timesheet> sheets;
  final List<String> tags;

  /// The server wasn't reachable: the week as it was loaded last.
  final bool fromCache;

  TimesheetWeek asFromCache() =>
      TimesheetWeek(monday, sheets, tags, fromCache: true);

  List<Timesheet> bookingsOf(DateTime day) {
    final d = dayOf(day);
    return [
      for (final it in sheets)
        if (dayOf(it.start) == d) it,
    ];
  }

  Duration totalOf(DateTime day) =>
      bookingsOf(day).fold(Duration.zero, (sum, it) => sum + it.duration);

  Duration get total =>
      sheets.fold(Duration.zero, (sum, it) => sum + it.duration);

  /// The gaps between the bookings of a day, and today from the last one up to now (at most 20:00, so the app
  /// opened late in the evening doesn't offer the night).
  List<Gap> gapsOf(DateTime day, DateTime now) {
    final list = bookingsOf(day);
    final gaps = <Gap>[];
    for (var i = 0; i + 1 < list.length; i++) {
      if (list[i + 1].start.difference(list[i].stop).inMinutes >=
          bookingStepMinutes) {
        gaps.add(Gap(list[i].stop, list[i + 1].start));
      }
    }
    if (list.isNotEmpty && dayOf(day) == dayOf(now)) {
      var until = floor5(now);
      final evening = dayOf(now).add(const Duration(hours: 20));
      if (until.isAfter(evening)) until = evening;
      if (until.difference(list.last.stop).inMinutes >= bookingStepMinutes) {
        gaps.add(Gap(list.last.stop, until));
      }
    }
    return gaps;
  }

  /// The start of a new booking: the end of the last booking of the day, or [defaultFirstHour] o'clock.
  DateTime nextStartOf(DateTime day) {
    final list = bookingsOf(day);
    return list.isEmpty
        ? dayOf(day).add(const Duration(hours: defaultFirstHour))
        : list.last.stop;
  }

  Map<String, dynamic> toJson() => {
    'timesheets': [for (final it in sheets) it.json],
    'tags': tags,
  };
}

/// A template of the user (`/rs/timesheet/favorites`, Kotlin `TimesheetFavorite`).
class TimesheetTemplate {
  const TimesheetTemplate({
    required this.id,
    required this.name,
    this.taskId,
    this.kost2Id,
    this.description = '',
    this.location = '',
    this.reference = '',
    this.tag,
  });

  factory TimesheetTemplate.fromJson(Map<String, dynamic> json) =>
      TimesheetTemplate(
        id: (json['id'] as num).toInt(),
        name: json['name'] as String? ?? '',
        taskId: (json['taskId'] as num?)?.toInt(),
        kost2Id: (json['cost2Id'] as num?)?.toInt(),
        description: json['description'] as String? ?? '',
        location: json['location'] as String? ?? '',
        reference: json['reference'] as String? ?? '',
        tag: (json['tag'] as String?)?.isEmpty ?? true
            ? null
            : json['tag'] as String?,
      );

  final int id;
  final String name;
  final int? taskId;
  final int? kost2Id;
  final String description;
  final String location;
  final String reference;
  final String? tag;
}

/// A task with the cost units a booking on it may charge (`GET /rs/task/info/{id}`).
class TaskInfo {
  const TaskInfo(this.task, this.kost2List);

  factory TaskInfo.fromJson(Map<String, dynamic> json) =>
      TaskInfo(TaskRef.fromJson(json), [
        for (final it in json['kost2List'] as List<dynamic>? ?? const [])
          Kost2Ref.fromJson(it as Map<String, dynamic>),
      ]);

  final TaskRef task;
  final List<Kost2Ref> kost2List;
}

/// What the booking form saves.
class BookingDraft {
  const BookingDraft({
    required this.task,
    this.kost2,
    required this.start,
    required this.stop,
    this.description = '',
    this.location = '',
    this.reference = '',
    this.tag,
  });

  final TaskRef task;
  final Kost2Ref? kost2;
  final DateTime start;
  final DateTime stop;
  final String description;
  final String location;
  final String reference;
  final String? tag;

  BookingWhat get what => BookingWhat(
    task: task,
    kost2: kost2,
    description: description,
    location: location,
    reference: reference,
    tag: tag,
  );
}

/// A running timer, kept on the device only.
class RunningTimer {
  const RunningTimer(this.start, this.what);

  factory RunningTimer.fromJson(Map<String, dynamic> json) => RunningTimer(
    DateTime.fromMillisecondsSinceEpoch(json['start'] as int),
    json['what'] == null
        ? null
        : BookingWhat.fromJson(json['what'] as Map<String, dynamic>),
  );

  final DateTime start;
  final BookingWhat? what;

  Map<String, dynamic> toJson() => {
    'start': start.millisecondsSinceEpoch,
    'what': what?.toJson(),
  };
}
