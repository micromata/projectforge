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

import 'dart:io';

import 'package:path_provider/path_provider.dart';

/// The temporary folder of the files the app hands to the share sheet (downloads of the pages, vCards). Emptied by
/// the logout: they may be confidential (chat files).
class SharedFiles {
  SharedFiles([Future<Directory> Function()? baseDir])
    : _baseDir = baseDir ?? getTemporaryDirectory;

  final Future<Directory> Function() _baseDir;

  /// The folder, created if needed.
  Future<Directory> directory() async {
    final dir = Directory('${(await _baseDir()).path}/shared');
    await dir.create(recursive: true);
    return dir;
  }

  Future<void> clearAll() async {
    final dir = Directory('${(await _baseDir()).path}/shared');
    if (await dir.exists()) await dir.delete(recursive: true);
  }
}
