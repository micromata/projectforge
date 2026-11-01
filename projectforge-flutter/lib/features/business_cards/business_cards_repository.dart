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

  /// A new card (not stored) with the data of the address.
  Future<BusinessCard> fromAddress(int addressId) async => BusinessCard(
    await _api.get<Map<String, dynamic>>(
      '$_base/fromAddress',
      query: {'id': addressId},
    ),
  );

  /// The QR code of the card as it is being edited; nothing is stored.
  Future<BusinessCardPreview> preview(BusinessCard card) async =>
      BusinessCardPreview.fromJson(
        await _api.post<Map<String, dynamic>>(
          '$_base/preview',
          data: card.json,
        ),
      );

  /// Inserts (no id) or updates the card. Throws an [ApiException] with the validation errors on a refusal.
  Future<BusinessCardInfo> save(BusinessCard card) async {
    final response = await _api.post<Map<String, dynamic>>(
      '$_base/save',
      data: {'data': card.json},
    );
    final variables = response['variables'] as Map<String, dynamic>;
    return BusinessCardInfo.fromJson(variables['data'] as Map<String, dynamic>);
  }

  Future<void> delete(String id) =>
      _api.post<void>('$_base/delete', query: {'id': id});

  /// The card as vCard file.
  Future<List<int>> vCard(String id) =>
      _api.download('$_base/vcf', query: {'id': id});

  /// Addresses readable by the user, for taking over the data of another one.
  Future<List<({int id, String displayName})>> searchAddresses(
    String search,
  ) async {
    final list = await _api.get<List<dynamic>>(
      '/rs/address/autosearch',
      query: {'search': search, 'maxResults': 20},
    );
    return list
        .cast<Map<String, dynamic>>()
        .map(
          (it) => (
            id: it['id'] as int,
            displayName: it['displayName'] as String? ?? '',
          ),
        )
        .toList();
  }
}
