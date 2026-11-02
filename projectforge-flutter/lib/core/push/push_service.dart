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

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../auth/auth_controller.dart';
import '../status/user_status_controller.dart';

/// Pushes (APNs) of the server wake the app for a notification. iOS only so far; the native part is in
/// `ios/Runner/AppDelegate.swift` (channel `projectforge/push`).
///
/// The push carries no text of the notification, only a generic one, its link and the count: the app loads the
/// content itself.
class PushService {
  PushService(
    this._api, {
    required this.onLink,
    required this.onReceived,
    MethodChannel? channel,
    bool? production,
  }) : _channel = channel ?? const MethodChannel('projectforge/push'),
       _production = production ?? kReleaseMode;

  final ApiClient _api;
  final MethodChannel _channel;

  /// Fallback if the signing is unknown: debug builds are signed for the sandbox of APNs, release builds (TestFlight,
  /// store) for production. A token is valid for one of them only.
  final bool _production;

  /// A push was tapped: its link (empty if it has none).
  final void Function(String link) onLink;

  /// A push arrived while the app is open.
  final void Function() onReceived;

  String? _sentToken;

  /// As the app is signed, told by the native part; null: [_production] decides.
  String? _signedEnvironment;

  /// Asks for the permission (the system asks once), registers at APNs and hands the token to the server. A push
  /// that started the app is passed to [onLink].
  Future<void> start() async {
    _channel.setMethodCallHandler(_onCall);
    final answer = await _channel.invokeMapMethod<String, dynamic>('start');
    final link = answer?['launchLink'] as String?;
    _signedEnvironment = answer?['environment'] as String?;
    if (link != null) onLink(link);
  }

  /// The number on the icon of the app.
  Future<void> setBadge(int count) => _channel
      .invokeMethod<void>('setBadge', count)
      .catchError((Object _) {}); // Not important.

  Future<dynamic> _onCall(MethodCall call) async {
    switch (call.method) {
      case 'token':
        await _register(call.arguments as String);
      case 'link':
        onLink(call.arguments as String? ?? '');
      case 'received':
        onReceived();
    }
  }

  Future<void> _register(String token) async {
    if (token == _sentToken) return;
    try {
      await _api.post<void>(
        '/rs/appAuth/pushToken',
        data: {
          'token': token,
          'environment':
              _signedEnvironment ?? (_production ? 'PRODUCTION' : 'SANDBOX'),
        },
        renewLogin: false,
      );
      _sentToken = token;
    } on ApiException {
      // Offline or refused: tried again with the next start of the app.
    }
  }
}

final pushServiceProvider = Provider<PushService?>((ref) {
  final api = ref.watch(apiClientProvider);
  if (api == null || !Platform.isIOS) return null;
  return PushService(
    api,
    onLink: (link) => ref.read(pushLinkProvider.notifier).set(link),
    onReceived: () => ref.read(userStatusProvider.notifier).refresh(),
  );
});

/// The link of a tapped push, until the home screen has followed it.
final pushLinkProvider = NotifierProvider<PushLinkNotifier, String?>(
  PushLinkNotifier.new,
);

class PushLinkNotifier extends Notifier<String?> {
  @override
  String? build() => null;

  void set(String link) => state = link;

  void clear() => state = null;
}
