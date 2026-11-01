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
  });

  factory Session.fromJson(Map<String, dynamic> json) => Session(
    serverUrl: json['serverUrl'] as String,
    token: json['token'] as String,
    userId: json['userId'] as int,
    deviceId: json['deviceId'] as int,
    username: json['username'] as String?,
    fullname: json['fullname'] as String?,
  );

  final String serverUrl;
  final String token;
  final int userId;
  final int deviceId;
  final String? username;
  final String? fullname;

  Session withToken(String token) => Session(
    serverUrl: serverUrl,
    token: token,
    userId: userId,
    deviceId: deviceId,
    username: username,
    fullname: fullname,
  );

  Map<String, dynamic> toJson() => {
    'serverUrl': serverUrl,
    'token': token,
    'userId': userId,
    'deviceId': deviceId,
    'username': username,
    'fullname': fullname,
  };
}

/// Keeps the [Session] in the key chain (iOS) or the keystore backed storage (Android), and the last server url
/// for the next login.
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
  static const _serverUrlKey = 'serverUrl';

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
    await _storage.write(key: _serverUrlKey, value: session.serverUrl);
  }

  Future<void> clear() => _storage.delete(key: _sessionKey);

  Future<String?> lastServerUrl() => _storage.read(key: _serverUrlKey);
}
