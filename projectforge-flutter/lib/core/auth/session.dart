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

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// The login of the app at its server: the token is the credential, everything else is for display.
class Session {
  const Session({
    required this.serverUrl,
    required this.token,
    required this.userId,
    required this.deviceId,
    this.username,
    this.fullname,
    this.proxyCookies = const {},
  });

  factory Session.fromJson(Map<String, dynamic> json) => Session(
    serverUrl: json['serverUrl'] as String,
    token: json['token'] as String,
    userId: json['userId'] as int,
    deviceId: json['deviceId'] as int,
    username: json['username'] as String?,
    fullname: json['fullname'] as String?,
    proxyCookies: proxyCookiesFromJson(json['proxyCookies']),
  );

  /// The `proxyCookies` of the server's answer or of a stored session (absent before this field existed).
  static Map<String, String> proxyCookiesFromJson(Object? json) => json is Map
      ? json.map((key, value) => MapEntry('$key', '$value'))
      : const {};

  final String serverUrl;
  final String token;
  final int userId;
  final int deviceId;
  final String? username;
  final String? fullname;

  /// The cookies of an SSO proxy in front of the server (e.g. Authentik), taken from the browser login: sent with
  /// each request and put into the WebView, otherwise the proxy refuses the app. Empty without such a proxy.
  final Map<String, String> proxyCookies;

  Session withToken(String token) => Session(
    serverUrl: serverUrl,
    token: token,
    userId: userId,
    deviceId: deviceId,
    username: username,
    fullname: fullname,
    proxyCookies: proxyCookies,
  );

  Session withProxyCookies(Map<String, String> proxyCookies) => Session(
    serverUrl: serverUrl,
    token: token,
    userId: userId,
    deviceId: deviceId,
    username: username,
    fullname: fullname,
    proxyCookies: proxyCookies,
  );

  Map<String, dynamic> toJson() => {
    'serverUrl': serverUrl,
    'token': token,
    'userId': userId,
    'deviceId': deviceId,
    'username': username,
    'fullname': fullname,
    'proxyCookies': proxyCookies,
  };
}

/// Keeps the [Session] in the key chain (iOS) or the keystore backed storage (Android), and the servers of the last
/// logins for the next one.
class SessionStore {
  SessionStore([FlutterSecureStorage? storage])
    : _storage =
          storage ??
          const FlutterSecureStorage(
            // Not in backups or on another device: the token belongs to this one.
            iOptions: IOSOptions(
              accessibility: KeychainAccessibility.first_unlock_this_device,
            ),
          );

  static const _sessionKey = 'session';

  /// The servers of the last logins, most recent first (urls only).
  static const _serversKey = 'servers';

  /// The single last server of older versions, read once for [servers].
  static const _legacyServerUrlKey = 'serverUrl';

  static const maxServers = 10;

  final FlutterSecureStorage _storage;

  Future<Session?> load() async {
    final json = await _storage.read(key: _sessionKey);
    if (json == null) return null;
    try {
      return Session.fromJson(jsonDecode(json) as Map<String, dynamic>);
    } on Object {
      await _storage.delete(
        key: _sessionKey,
      ); // Written by an older version: log in again.
      return null;
    }
  }

  Future<void> save(Session session) async {
    await _storage.write(key: _sessionKey, value: jsonEncode(session.toJson()));
    final servers = (await this.servers())
      ..remove(session.serverUrl)
      ..insert(0, session.serverUrl);
    await _writeServers(servers.take(maxServers).toList());
  }

  /// Forgets the session, but not the servers: they are offered by the next login.
  Future<void> clear() => _storage.delete(key: _sessionKey);

  /// The servers of the last logins, most recent first.
  Future<List<String>> servers() async {
    final json = await _storage.read(key: _serversKey);
    if (json == null) {
      final legacy = await _storage.read(key: _legacyServerUrlKey);
      return legacy == null ? [] : [legacy];
    }
    try {
      return (jsonDecode(json) as List).cast<String>().toList();
    } on Object {
      return [];
    }
  }

  Future<void> removeServer(String serverUrl) async {
    final servers = (await this.servers())..remove(serverUrl);
    await _writeServers(servers);
  }

  Future<void> _writeServers(List<String> servers) async {
    await _storage.write(key: _serversKey, value: jsonEncode(servers));
    await _storage.delete(key: _legacyServerUrlKey);
  }
}
