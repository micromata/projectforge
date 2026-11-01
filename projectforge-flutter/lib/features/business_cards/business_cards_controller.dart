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

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/auth/auth_controller.dart';
import 'business_card.dart';
import 'business_cards_repository.dart';

final businessCardsRepositoryProvider = Provider<BusinessCardsRepository?>((
  ref,
) {
  final api = ref.watch(apiClientProvider);
  return api == null
      ? null
      : BusinessCardsRepository(api, ref.watch(jsonCacheProvider));
});

final businessCardsProvider =
    AsyncNotifierProvider<BusinessCardsController, BusinessCards>(
      BusinessCardsController.new,
    );

/// The list of the own business cards (created and edited in the web version).
///
/// The stored cards are shown at once (also without network) and replaced by the ones of the server when it
/// answers; if it doesn't, the stored ones stay.
class BusinessCardsController extends AsyncNotifier<BusinessCards> {
  @override
  Future<BusinessCards> build() async {
    final repository = ref.watch(businessCardsRepositoryProvider)!;
    final cached = await repository.loadCached();
    if (cached == null) return repository.load();
    _reloadSilently(repository);
    return cached;
  }

  BusinessCardsRepository get _repository =>
      ref.read(businessCardsRepositoryProvider)!;

  Future<void> _reloadSilently(BusinessCardsRepository repository) async {
    try {
      final cards = await repository.load();
      if (ref.mounted) state = AsyncData(cards);
    } on Object {
      // The stored cards stay.
    }
  }

  /// Loads the cards from the server. Without a connection the stored cards stay (marked as such); another failure
  /// keeps the shown cards too and is returned for the caller to report.
  Future<ApiException?> refresh() async {
    final shown = state.value;
    try {
      state = AsyncData(await _repository.load());
      return null;
    } on ApiException catch (e) {
      if (shown == null) state = AsyncError(e, StackTrace.current);
      return e;
    }
  }
}
