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

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import 'pkce.dart';
import 'session.dart';

/// The user closed the browser or declined the app.
class AppLoginCancelled implements Exception {}

/// The user typed something that is no server url.
class InvalidServerUrl implements Exception {}

/// A server url with plain http in a release build: the token would travel in clear.
class InsecureServerUrl implements Exception {}

/// The login of the app (see AppAuthService of the server): the login page of the server opens in the system
/// browser, so password, WebAuthn and OTP work as in the web, for any domain. Its callback
/// (`projectforge://auth?code=…&state=…`) carries a one-time code, exchanged here with the PKCE verifier for the
/// token of the app.
class AppLogin {
  AppLogin({Dio? dio, Future<String> Function(String url)? openBrowser})
    : _dio = dio ?? Dio(),
      _openBrowser = openBrowser ?? _authenticateInBrowser;

  static const callbackScheme = 'projectforge';

  final Dio _dio;
  final Future<String> Function(String url) _openBrowser;

  /// @param deviceId The id of this app at the server if it is already registered (a new second factor): the
  /// server keeps its device and only renews its token.
  Future<Session> login({
    required String serverUrl,
    required String deviceName,
    required String platform,
    int? deviceId,
  }) async {
    final pkce = Pkce.create();
    final url = Uri.parse('$serverUrl/next/appLogin').replace(
      queryParameters: {
        'challenge': pkce.challenge,
        'state': pkce.state,
        'device': deviceName,
        'platform': platform,
        if (deviceId != null) 'deviceId': '$deviceId',
      },
    );
    final String result;
    try {
      result = await _openBrowser(url.toString());
    } on PlatformException catch (e) {
      if (e.code == 'CANCELED') throw AppLoginCancelled();
      rethrow;
    }
    final callback = Uri.parse(result);
    final params = callback.queryParameters;
    if (params['state'] != pkce.state) {
      throw ApiException('Invalid state of the login callback.');
    }
    if (params['error'] != null) {
      throw AppLoginCancelled();
    }
    final code = params['code'];
    if (code == null) {
      throw ApiException('No code in the login callback.');
    }
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '$serverUrl/rsPublic/appAuth/token',
        data: {'code': code, 'codeVerifier': pkce.verifier},
      );
      final json = response.data!;
      return Session(
        serverUrl: serverUrl,
        token: json['token'] as String,
        userId: json['userId'] as int,
        deviceId: json['deviceId'] as int,
        username: json['username'] as String?,
        fullname: json['fullname'] as String?,
      );
    } on DioException catch (e) {
      throw ApiClient.toApiException(e);
    }
  }

  /// The public system status of the server (`/rsPublic/systemStatus`): whether it is a ProjectForge at all, its
  /// name and version.
  Future<Map<String, dynamic>> systemStatus(String serverUrl) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '$serverUrl/rsPublic/systemStatus',
      );
      return response.data ?? {};
    } on DioException catch (e) {
      throw ApiClient.toApiException(e);
    }
  }

  /// `https://pf.example.com/next/…` or `pf.example.com` → `https://pf.example.com`; null if it is no url.
  static String? normalizeServerUrl(String input) {
    var value = input.trim();
    if (value.isEmpty) return null;
    if (!value.contains('://')) value = 'https://$value';
    final uri = Uri.tryParse(value);
    if (uri == null ||
        uri.host.isEmpty ||
        (uri.scheme != 'https' && uri.scheme != 'http')) {
      return null;
    }
    var path = uri.path;
    for (final suffix in ['/next', '/react', '/wa']) {
      final index = path.indexOf(suffix);
      if (index >= 0) path = path.substring(0, index);
    }
    path = path.replaceAll(RegExp(r'/+$'), '');
    return uri
        .replace(path: path, query: null, fragment: null)
        .toString()
        .replaceAll(RegExp(r'[?#]$'), '');
  }

  /// Plain http is only for the development servers (debug builds): the code, its verifier and the token would
  /// otherwise travel in clear. Dio (dart:io) isn't bound by iOS ATS nor by the cleartext policy of Android.
  static bool isInsecure(String serverUrl, {bool allowHttp = kDebugMode}) =>
      !allowHttp && Uri.parse(serverUrl).scheme == 'http';

  static Future<String> _authenticateInBrowser(String url) =>
      FlutterWebAuth2.authenticate(url: url, callbackUrlScheme: callbackScheme);
}
