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

import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// Json documents the app shows when the server isn't reachable (e.g. the business cards at a fair without
/// network). Kept in the app's support directory, which no other app reads; deleted on logout.
class JsonCache {
  JsonCache([Future<Directory> Function()? baseDir])
    : _baseDir = baseDir ?? getApplicationSupportDirectory;

  final Future<Directory> Function() _baseDir;

  Future<Map<String, dynamic>?> read(String name) async {
    final file = await _file(name);
    if (!await file.exists()) return null;
    try {
      return jsonDecode(await file.readAsString()) as Map<String, dynamic>;
    } on Object {
      return null; // Broken or of an older version: as if there were none.
    }
  }

  Future<void> write(String name, Map<String, dynamic> json) async {
    final file = await _file(name);
    await file.parent.create(recursive: true);
    await file.writeAsString(jsonEncode(json), flush: true);
  }

  Future<void> delete(String name) async {
    final file = await _file(name);
    if (await file.exists()) await file.delete();
  }

  /// A directory of the cache for files of its own (images), deleted with it.
  Future<Directory> directory(String name) async {
    final dir = Directory('${(await _baseDir()).path}/cache/$name');
    await dir.create(recursive: true);
    return dir;
  }

  Future<void> clearAll() async {
    final dir = Directory('${(await _baseDir()).path}/cache');
    if (await dir.exists()) await dir.delete(recursive: true);
  }

  Future<File> _file(String name) async =>
      File('${(await _baseDir()).path}/cache/$name.json');
}
