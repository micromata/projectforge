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

import '../../core/api/api_client.dart';
import '../../core/api/api_exception.dart';
import '../../core/cache/json_cache.dart';
import 'business_card.dart';

/// The own business cards of the user (BusinessCardRest, `/rs/myBusinessCards`). The list is cached: a card has to
/// be shown where there is no network.
class BusinessCardsRepository {
  BusinessCardsRepository(this._api, this._cache);

  static const _base = '/rs/myBusinessCards';
  static const _cacheName = 'businessCards';

  final ApiClient _api;
  final JsonCache _cache;

  /// The cards from the server, or the cached ones if it isn't reachable.
  Future<BusinessCards> load() async {
    try {
      final json = await _api.get<Map<String, dynamic>>(_base);
      await _cache.write(_cacheName, json);
      return BusinessCards.fromJson(json);
    } on ApiException catch (e) {
      if (!e.isOffline) rethrow;
      final cached = await _cache.read(_cacheName);
      if (cached == null) rethrow;
      return BusinessCards.fromJson(cached, fromCache: true);
    }
  }

  /// The card as vCard file.
  Future<List<int>> vCard(String id) =>
      _api.download('$_base/vcf', query: {'id': id});
}
