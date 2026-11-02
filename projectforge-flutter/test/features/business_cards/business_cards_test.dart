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

import 'package:dio/dio.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/auth/auth_controller.dart';
import 'package:projectforge_app/core/auth/session.dart';
import 'package:projectforge_app/core/cache/json_cache.dart';
import 'package:projectforge_app/features/business_cards/business_cards_controller.dart';

import 'package:projectforge_app/features/business_cards/business_card.dart';
import 'package:projectforge_app/core/wallet/wallet_pass.dart';
import 'package:projectforge_app/features/business_cards/wallet_pass.dart';

import '../../core/fake_adapter.dart';

class _MemorySessionStore extends SessionStore {
  @override
  Future<Session?> load() async => const Session(
    serverUrl: 'https://pf.example.com',
    token: 't',
    userId: 1,
    deviceId: 10,
  );
}

Map<String, dynamic> _cards(String name, {bool wallet = true}) => {
  'cards': [
    {
      'card': {'id': 'c1', 'cardName': name, 'lastName': 'Doe'},
      'preview': {'qrCodeUrl': 'data:image/png;base64,AAAA'},
    },
  ],
  'walletConfigured': wallet,
};

void main() {
  late Directory dir;
  late FakeAdapter api;
  var online = true;

  ProviderContainer container() {
    late ProviderContainer c;
    c = ProviderContainer(
      overrides: [
        sessionStoreProvider.overrideWithValue(_MemorySessionStore()),
        jsonCacheProvider.overrideWithValue(JsonCache(() async => dir)),
        apiClientProvider.overrideWith(
          (ref) => ApiClient(
            serverUrl: 'https://pf.example.com',
            token: () => 't',
            appVersion: '1',
            onUnauthorized: () {},
            onReauthenticate: () async => false,
            dio: Dio()..httpClientAdapter = api,
          ),
        ),
      ],
    );
    addTearDown(c.dispose);
    return c;
  }

  Future<ProviderContainer> loggedIn() async {
    final c = container();
    c.read(authControllerProvider);
    await Future<void>.delayed(Duration.zero);
    return c;
  }

  setUp(() async {
    dir = await Directory.systemTemp.createTemp('pf_cards_test');
    online = true;
    api = FakeAdapter(
      (o) => online
          ? (status: 200, body: _cards('Server'))
          : throw DioException(
              requestOptions: o,
              type: DioExceptionType.connectionError,
            ),
    );
  });

  tearDown(() => dir.delete(recursive: true));

  test('the first load comes from the server and is stored', () async {
    final c = await loggedIn();
    final cards = await c.read(businessCardsProvider.future);
    expect(cards.fromCache, isFalse);
    expect(cards.walletConfigured, isTrue);
    expect(await JsonCache(() async => dir).read('businessCards'), isNotNull);
  });

  test('stored cards are shown first, the server replaces them', () async {
    await JsonCache(() async => dir).write('businessCards', _cards('Stored'));
    final c = await loggedIn();
    final first = await c.read(businessCardsProvider.future);
    expect(first.cards.single.card.cardName, 'Stored');
    await Future<void>.delayed(const Duration(milliseconds: 50));
    expect(
      c.read(businessCardsProvider).value!.cards.single.card.cardName,
      'Server',
    );
  });

  test('without the server the stored cards stay, silently', () async {
    await JsonCache(() async => dir).write('businessCards', _cards('Stored'));
    online = false;
    final c = await loggedIn();
    await c.read(businessCardsProvider.future);
    await Future<void>.delayed(const Duration(milliseconds: 50));
    final state = c.read(businessCardsProvider);
    expect(state.hasError, isFalse);
    expect(state.value!.cards.single.card.cardName, 'Stored');
    expect(state.value!.fromCache, isTrue);
    expect(await c.read(businessCardsProvider.notifier).refresh(), isNull);
  });

  test('the Wallet pass is loaded from the pkpass endpoint', () async {
    final c = await loggedIn();
    await c.read(businessCardsProvider.future);
    await c.read(businessCardsRepositoryProvider)!.pass('c1');
    final request = api.requests.last;
    expect(request.path, '/rs/myBusinessCards/pkpass');
    expect(request.queryParameters['id'], 'c1');
  });

  test(
    'the pass goes to the Wallet channel, a refusal shows its reason',
    () async {
      TestWidgetsFlutterBinding.ensureInitialized();
      api = FakeAdapter((_) => (status: 200, body: _cards('Server')));
      final c = await loggedIn();
      await c.read(businessCardsProvider.future);
      final repository = c.read(businessCardsRepositoryProvider)!;
      final card = BusinessCard({'id': 'c1'});
      Object? received;
      var refuse = false;
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(
            const MethodChannel('projectforge/wallet'),
            (call) async {
              received = call.arguments;
              if (refuse) {
                throw PlatformException(
                  code: 'invalid',
                  message: 'bad signature',
                );
              }
              return null;
            },
          );
      addTearDown(
        () => TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
            .setMockMethodCallHandler(
              const MethodChannel('projectforge/wallet'),
              null,
            ),
      );
      await addToWallet(repository, card);
      expect(received, isNotNull);
      refuse = true;
      await expectLater(
        addToWallet(repository, card),
        throwsA(
          isA<WalletException>().having(
            (e) => e.message,
            'message',
            'bad signature',
          ),
        ),
      );
    },
  );
}
