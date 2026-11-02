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

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';

import '../api/api_client.dart';
import '../api/api_exception.dart';
import '../log/app_log.dart';
import '../log/log_sanitizer.dart';
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
  /// @param loginPath The server takes the packed parameters in the path too ([Session.appLoginPath]).
  Future<Session> login({
    required String serverUrl,
    required String deviceName,
    required String platform,
    int? deviceId,
    bool loginPath = false,
  }) async {
    final pkce = Pkce.create();
    final loginParams = {
      'challenge': pkce.challenge,
      'state': pkce.state,
      'device': deviceName,
      'platform': platform,
      if (deviceId != null) 'deviceId': '$deviceId',
    };
    final packed = packParams(loginParams);
    final url = Uri.parse(
      '$serverUrl/next/appLogin${loginPath ? '/$packed' : ''}',
    ).replace(queryParameters: {'login': packed, ...loginParams});
    appLog.info(
      'login',
      'Browser login${deviceId != null ? ' (renewal of device $deviceId)' : ''}: ${LogSanitizer.url(url)}',
    );
    final String result;
    try {
      result = await _openBrowser(url.toString());
    } on PlatformException catch (e) {
      if (e.code == 'CANCELED') {
        appLog.info('login', 'Browser login cancelled');
        throw AppLoginCancelled();
      }
      appLog.error('login', 'Browser login failed', e);
      rethrow;
    }
    final callback = Uri.parse(result);
    appLog.info('login', 'Callback: ${LogSanitizer.url(callback)}');
    final params = callback.queryParameters;
    if (params['state'] != pkce.state) {
      appLog.warn('login', 'Callback with another state');
      throw ApiException('Invalid state of the login callback.');
    }
    if (params['error'] != null) {
      appLog.info('login', 'Callback with error: ${params['error']}');
      throw AppLoginCancelled();
    }
    final code = params['code'];
    if (code == null) {
      appLog.warn('login', 'Callback without code');
      throw ApiException('No code in the login callback.');
    }
    try {
      final response = await _dio.post<Map<String, dynamic>>(
        '$serverUrl/rsPublic/appAuth/token',
        data: {'code': code, 'codeVerifier': pkce.verifier},
      );
      _logResponse('Token', response);
      final json = response.data!;
      final proxyCookies = Session.proxyCookiesFromJson(json['proxyCookies']);
      // Their values are credentials of the proxy, whatever the server named them.
      LogSanitizer.addSecretNames(proxyCookies.keys);
      appLog.info(
        'login',
        'Logged in: user ${json['userId']}, device ${json['deviceId']}, proxy cookies: '
            '${LogSanitizer.cookieNames(proxyCookies.keys)}',
      );
      return Session(
        serverUrl: serverUrl,
        token: json['token'] as String,
        userId: json['userId'] as int,
        deviceId: json['deviceId'] as int,
        username: json['username'] as String?,
        fullname: json['fullname'] as String?,
        proxyCookies: proxyCookies,
        appLoginPath: json['appLoginPath'] == true,
      );
    } on DioException catch (e) {
      _logFailure('Token', e);
      throw ApiClient.toApiException(e);
    }
  }

  /// [params] packed into one query parameter (base64url of a json object, without padding), read by the
  /// `appLogin` page of next (readAppLoginParams) before the single ones. It goes first: an SSO proxy redirecting
  /// the browser to its login and back may keep only the first parameter (an unencoded `rd=…?a=1&b=2`). The single
  /// ones stay for servers of an older version.
  @visibleForTesting
  static String packParams(Map<String, String> params) => base64Url
      .encode(
        utf8.encode(
          jsonEncode({
            ...params,
            if (params['deviceId'] != null)
              'deviceId': int.parse(params['deviceId']!),
          }),
        ),
      )
      .replaceAll('=', '');

  /// The public system status of the server (`/rsPublic/systemStatus`): whether it is a ProjectForge at all, its
  /// name and version.
  Future<Map<String, dynamic>> systemStatus(String serverUrl) async {
    try {
      final response = await _dio.get<Map<String, dynamic>>(
        '$serverUrl/rsPublic/systemStatus',
      );
      _logResponse('System status', response);
      final data = response.data ?? {};
      appLog.info(
        'login',
        'Server ${LogSanitizer.urlString(serverUrl)}: ${data['appname'] ?? '-'} ${data['version'] ?? ''}',
      );
      return data;
    } on DioException catch (e) {
      _logFailure('System status', e);
      throw ApiClient.toApiException(e);
    }
  }

  /// The redirects (e.g. through an SSO proxy) the public requests followed.
  static void _logResponse(String what, Response<dynamic> response) {
    final redirects = response.redirects
        .map((it) => '${it.statusCode} ${LogSanitizer.url(it.location)}')
        .join(' → ');
    appLog.debug(
      'login',
      '$what → ${response.statusCode}${redirects.isEmpty ? '' : ' after redirects: $redirects'}',
    );
  }

  static void _logFailure(String what, DioException e) {
    final response = e.response;
    appLog.warn(
      'login',
      response == null
          ? '$what failed: ${e.type.name}${e.message != null ? ' (${e.message})' : ''}'
          : '$what → ${response.statusCode} '
                '(content-type: ${response.headers.value(Headers.contentTypeHeader) ?? '-'}, '
                'url: ${LogSanitizer.url(response.realUri)})',
    );
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
