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

import 'dart:io';

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/cache/json_cache.dart';
import 'address.dart';

/// The address book (AddressEntityRest, `/rs/address/appList`). All addresses the user may see are kept on the
/// device, with their thumbnails, so an address can also be used where there is no network; the server only
/// delivers what changed since the last sync.
class AddressesRepository {
  AddressesRepository(this._api, this._cache);

  static const _path = '/rs/address';
  static const _cacheName = 'addresses';
  static const _imageDir = 'addressImages';

  final ApiClient _api;
  final JsonCache _cache;

  /// The stored address book, null if there is none (yet). Not marked as offline: the server isn't asked.
  Future<AddressBook?> loadCached() async {
    final cached = await _cache.read(_cacheName);
    return cached == null ? null : AddressBook.fromJson(cached);
  }

  /// Brings [stored] up to date. Without a connection it stays as it is (marked as such); another failure is thrown.
  Future<AddressBook> sync(AddressBook? stored) async {
    try {
      var book = stored == null ? null : await _syncChanges(stored);
      book ??= await _loadAll();
      await _cache.write(_cacheName, book.toJson());
      await _deleteObsoleteImages(book);
      return book;
    } on ApiException catch (e) {
      if (!e.isOffline || stored == null) rethrow;
      return stored.asFromCache();
    }
  }

  Future<AddressBook?> _syncChanges(AddressBook stored) async {
    final json = await _api.get<Map<String, dynamic>>(
      '$_path/appList',
      query: {'modifiedSince': stored.timestamp},
    );
    return stored.merge(
      json['timestamp'] as int,
      (json['ids'] as List<dynamic>).cast<int>(),
      _addresses(json),
      favoriteIds: _favoriteIds(json),
    );
  }

  Future<AddressBook> _loadAll() async {
    final json = await _api.get<Map<String, dynamic>>('$_path/appList');
    return AddressBook(
      json['timestamp'] as int,
      _addresses(json),
      favoriteIds: {..._favoriteIds(json)},
    );
  }

  static List<int> _favoriteIds(Map<String, dynamic> json) =>
      (json['favoriteIds'] as List<dynamic>? ?? []).cast<int>();

  static List<Address> _addresses(Map<String, dynamic> json) => [
    for (final it in json['addresses'] as List<dynamic>)
      Address.fromJson(it as Map<String, dynamic>),
  ];

  /// The thumbnail of the address ([full]: the image itself), from the device or (once) from the server. Null if there is none, or it isn't
  /// reachable now.
  Future<File?> image(int id, int version, {bool full = false}) async {
    final dir = await _cache.directory(_imageDir);
    final file = File('${dir.path}/${_imageName(id, version, full: full)}');
    if (await file.exists()) return file;
    try {
      final bytes = await _api.download(
        '$_path/${full ? 'image' : 'imagePreview'}/$id',
      );
      await file.writeAsBytes(bytes, flush: true);
      return file;
    } on ApiException {
      return null;
    }
  }

  static String _imageName(int id, int version, {bool full = false}) =>
      '$id-$version${full ? '-full' : ''}.img';

  /// Deletes the thumbnails of removed addresses and older versions.
  Future<void> _deleteObsoleteImages(AddressBook book) async {
    final current = {
      for (final it in book.addresses)
        if (it.hasImage) ...[
          _imageName(it.id, it.lastUpdate),
          _imageName(it.id, it.lastUpdate, full: true),
        ],
    };
    final dir = await _cache.directory(_imageDir);
    await for (final entry in dir.list()) {
      if (entry is! File) continue;
      if (!current.contains(entry.uri.pathSegments.last)) {
        await entry.delete();
      }
    }
  }
}
