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
class BusinessCardsController extends AsyncNotifier<BusinessCards> {
  @override
  Future<BusinessCards> build() =>
      ref.watch(businessCardsRepositoryProvider)!.load();

  BusinessCardsRepository get _repository =>
      ref.read(businessCardsRepositoryProvider)!;

  Future<void> refresh() async {
    state = await AsyncValue.guard(_repository.load);
  }
}
