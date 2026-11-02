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

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/auth/auth_controller.dart';
import 'address.dart';
import 'addresses_repository.dart';

final addressesRepositoryProvider = Provider<AddressesRepository?>((ref) {
  final api = ref.watch(apiClientProvider);
  return api == null
      ? null
      : AddressesRepository(api, ref.watch(jsonCacheProvider));
});

final addressesProvider =
    AsyncNotifierProvider<AddressesController, AddressBook>(
      AddressesController.new,
    );

/// The thumbnail of an address, or with `full` its image itself (null: none); the family key holds the address and
/// its version, so a changed image is loaded again.
final addressImageProvider =
    FutureProvider.family<File?, ({Address address, bool full})>(
      (ref, key) => key.address.hasImage
          ? ref
                .watch(addressesRepositoryProvider)
                ?.image(key.address.id, key.address.lastUpdate, full: key.full)
          : null,
    );

/// The address book of the app: the stored addresses are shown at once (also without network) and replaced by the
/// synced ones when the server answers; if it doesn't, the stored ones stay.
class AddressesController extends AsyncNotifier<AddressBook> {
  @override
  Future<AddressBook> build() async {
    final repository = ref.watch(addressesRepositoryProvider)!;
    final cached = await repository.loadCached();
    if (cached == null) return repository.sync(null);
    _syncSilently(repository, cached);
    return cached;
  }

  AddressesRepository get _repository => ref.read(addressesRepositoryProvider)!;

  Future<void> _syncSilently(
    AddressesRepository repository,
    AddressBook stored,
  ) async {
    try {
      final book = await repository.sync(stored);
      if (ref.mounted) state = AsyncData(book);
    } on Object {
      // The stored addresses stay.
    }
  }

  /// Asks the server for changes. Without a connection the stored addresses stay (marked as such); another failure
  /// keeps the shown addresses too and is returned for the caller to report.
  Future<ApiException?> refresh() async {
    final shown = state.value;
    try {
      state = AsyncData(await _repository.sync(shown));
      return null;
    } on ApiException catch (e) {
      if (shown == null) state = AsyncError(e, StackTrace.current);
      return e;
    }
  }

  /// A silent [refresh], e.g. when the tab is opened; at most every few minutes.
  Future<void> refreshIfStale() async {
    final shown = state.value;
    if (shown == null) return;
    final now = DateTime.now();
    final last = _lastSync;
    if (last != null && now.difference(last) < const Duration(minutes: 3)) {
      return;
    }
    _lastSync = now;
    await _syncSilently(_repository, shown);
  }

  DateTime? _lastSync;
}
