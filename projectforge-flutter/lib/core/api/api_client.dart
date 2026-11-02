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

import '../log/app_log.dart';
import '../log/log_sanitizer.dart';
import 'api_exception.dart';

/// The calls of the app to its ProjectForge server (`/rs`, authenticated by the app token).
///
/// - Every request carries `Authorization: PFApp <token>`: no session, no cookies, hence no CSRF token.
/// - `X-PF-Frontend: next` selects the lean list rows projectforge-next gets.
/// - A 401 of the server (json body) means the token is gone (logged out in the web, expired, password changed):
///   [onUnauthorized].
/// - A 403 with `twoFactorRequired` means the second factor of the app's login is too old for this url: the app
///   logs in again in the browser ([onReauthenticate]) and repeats the request once with the new token.
/// - An SSO proxy in front of the server (e.g. Authentik) gets its cookies ([proxyCookies]). A refusal of the proxy
///   (a 401 without a json body, a redirect to its login) means they expired: the app logs in again in the browser,
///   which brings fresh ones, and repeats the request once.
/// - Requests the app makes on its own (polling) pass `renewLogin: false`: a browser login opens only for something
///   the user does, never out of the blue (and not again every minute after the user cancelled it).
class ApiClient {
  ApiClient({
    required this.serverUrl,
    required this.token,
    required this.appVersion,
    required this.onUnauthorized,
    required this.onReauthenticate,
    Map<String, String> Function()? proxyCookies,
    Dio? dio,
  }) : proxyCookies = proxyCookies ?? _noCookies,
       dio = dio ?? Dio() {
    this.dio.options
      ..baseUrl = serverUrl
      ..connectTimeout = const Duration(seconds: 15)
      ..receiveTimeout = const Duration(seconds: 30)
      // The api never redirects: the Authorization header must not follow a redirect elsewhere.
      ..followRedirects = false;
    this.dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: _onRequest,
        onResponse: _onResponse,
        onError: _onError,
      ),
    );
  }

  static const _retriedKey = 'pfRetried';
  static const _noRenewalKey = 'pfNoRenewal';
  static const _startKey = 'pfStart';

  /// The text of a refusal by an SSO proxy, see [toApiException].
  static const proxyRefusedKey = 'app.proxy.refused';

  static Map<String, String> _noCookies() => const {};

  final String serverUrl;
  final String appVersion;
  final Dio dio;
  final void Function() onUnauthorized;

  /// The current token of the app, read for every request: a new login replaces it without a new client.
  final String? Function() token;

  /// Does a new login in the browser; answers false if the user cancelled.
  final Future<bool> Function() onReauthenticate;

  /// The current cookies of the SSO proxy in front of the server (name to value), empty without one.
  final Map<String, String> Function() proxyCookies;

  /// The proxy cookies a new login in the browser brought without satisfying the proxy (e.g. no proxy cookies
  /// configured at the server): while the app has just these, the proxy's refusals aren't answered by a new browser
  /// login again, which would only open it on every request. A login that brings others may try again.
  Map<String, String>? _uselessProxyCookies;

  bool get _proxyRenewalUseless {
    final useless = _uselessProxyCookies;
    return useless != null && mapEquals(useless, proxyCookies());
  }

  /// Whether the SSO proxy refuses the pages right now: tells its login from any other site the WebView is
  /// redirected to (e.g. by an open redirect of the server). Asks for the start page of next (`/next/`, static, never
  /// redirected by the server itself) with the proxy cookies only, no token and no session, without following a
  /// redirect: a proxy that lets the app's `/rs` through but guards the pages is told apart too.
  Future<bool> proxyRefuses() async {
    final cookies = proxyCookies();
    final probe = Dio(
      BaseOptions(
        baseUrl: serverUrl,
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 15),
        followRedirects: false,
        validateStatus: (_) => true,
        responseType: ResponseType.plain,
        headers: {
          if (cookies.isNotEmpty)
            'Cookie': cookies.entries
                .map((it) => '${it.key}=${it.value}')
                .join('; '),
        },
      ),
    )..httpClientAdapter = dio.httpClientAdapter;
    try {
      final response = await probe.get<String>('/next/');
      final refused = isProxyRefusal(response);
      appLog.info(
        'api',
        'Probe of the SSO proxy: GET /next/ → ${response.statusCode}${refused ? ' (refused)' : ''}',
      );
      return refused;
    } on DioException catch (e) {
      appLog.warn('api', 'Probe of the SSO proxy failed: ${e.type.name}');
      return false;
    }
  }

  /// @param renewLogin False: a refusal doesn't start a new login in the browser (polling in the background).
  Future<T> get<T>(
    String path, {
    Map<String, dynamic>? query,
    bool renewLogin = true,
  }) => _call(
    () =>
        dio.get<T>(path, queryParameters: query, options: _options(renewLogin)),
  );

  /// @param renewLogin False: a refusal doesn't start a new login in the browser (the logout).
  Future<T> post<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? query,
    bool renewLogin = true,
  }) => _call(
    () => dio.post<T>(
      path,
      data: data,
      queryParameters: query,
      options: _options(renewLogin),
    ),
  );

  static Options? _options(bool renewLogin) =>
      renewLogin ? null : Options(extra: {_noRenewalKey: true});

  /// As [post], with the whole response (its headers, e.g. `set-cookie`).
  Future<Response<T>> postForResponse<T>(String path, {Object? data}) async {
    try {
      return await dio.post<T>(path, data: data);
    } on DioException catch (e) {
      throw toApiException(e);
    }
  }

  Future<T> put<T>(String path, {Object? data}) =>
      _call(() => dio.put<T>(path, data: data));

  Future<T> delete<T>(String path, {Object? data}) =>
      _call(() => dio.delete<T>(path, data: data));

  /// A file (vCard, …) as bytes.
  Future<List<int>> download(String path, {Map<String, dynamic>? query}) =>
      _call(
        () => dio.get<List<int>>(
          path,
          queryParameters: query,
          options: Options(responseType: ResponseType.bytes),
        ),
      );

  Future<T> _call<T>(Future<Response<T>> Function() request) async {
    try {
      return (await request()).data as T;
    } on DioException catch (e) {
      throw toApiException(e);
    }
  }

  void _onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    options.headers['Authorization'] = 'PFApp ${token()}';
    options.headers['X-PF-Frontend'] = 'next';
    options.headers['X-PF-App-Version'] = appVersion;
    final cookies = proxyCookies();
    if (cookies.isNotEmpty) {
      options.headers['Cookie'] = cookies.entries
          .map((it) => '${it.key}=${it.value}')
          .join('; ');
    }
    options.extra[_startKey] = DateTime.now();
    handler.next(options);
  }

  void _onResponse(
    Response<dynamic> response,
    ResponseInterceptorHandler handler,
  ) {
    // Polling would fill the log, only its failures are of interest.
    if (response.requestOptions.extra[_noRenewalKey] != true) {
      appLog.debug(
        'api',
        '${_describe(response.requestOptions)} → ${response.statusCode}${_duration(response.requestOptions)}',
      );
    }
    handler.next(response);
  }

  Future<void> _onError(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final response = error.response;
    final extra = error.requestOptions.extra;
    final proxyRefused = isProxyRefusal(response);
    _logError(error, proxyRefused);
    if (response?.statusCode == 401 && !proxyRefused) {
      appLog.warn('api', 'Token refused by the server: logged out');
      onUnauthorized();
    } else if ((proxyRefused && !_proxyRenewalUseless ||
            response?.statusCode == 403 &&
                _isTwoFactorRequired(response?.data)) &&
        extra[_retriedKey] != true &&
        extra[_noRenewalKey] != true) {
      appLog.info(
        'api',
        proxyRefused
            ? 'Refused by the SSO proxy: new login in the browser'
            : 'Second factor required: new login in the browser',
      );
      if (await onReauthenticate()) {
        final options = error.requestOptions..extra[_retriedKey] = true;
        try {
          return handler.resolve(await dio.fetch(options));
        } on DioException catch (retryError) {
          if (proxyRefused && isProxyRefusal(retryError.response)) {
            appLog.warn(
              'api',
              'Still refused by the SSO proxy after the new login: no further logins for this session',
            );
            _uselessProxyCookies = proxyCookies();
          }
          return handler.next(retryError);
        }
      }
      appLog.info('api', 'New login cancelled or failed');
    } else if (proxyRefused && _proxyRenewalUseless) {
      appLog.debug('api', "No new login: a login didn't satisfy the SSO proxy");
    }
    handler.next(error);
  }

  /// Method and url (query values masked) of a request.
  static String _describe(RequestOptions options) =>
      '${options.method} ${LogSanitizer.url(options.uri)}${options.extra[_retriedKey] == true ? ' (retry)' : ''}';

  static String _duration(RequestOptions options) {
    final start = options.extra[_startKey];
    return start is DateTime
        ? ' (${DateTime.now().difference(start).inMilliseconds} ms)'
        : '';
  }

  /// What a failure looks like, without bodies, header values or cookie values: enough to tell the server from a
  /// proxy (content type, redirect target, which cookies were sent).
  void _logError(DioException error, bool proxyRefused) {
    final options = error.requestOptions;
    final response = error.response;
    final request = _describe(options);
    if (response == null) {
      appLog.warn(
        'api',
        '$request${_duration(options)} failed: ${error.type.name}${error.message != null ? ' (${error.message})' : ''}',
      );
      return;
    }
    final headers = response.headers;
    final details = [
      'content-type: ${headers.value(Headers.contentTypeHeader) ?? '-'}',
      if (headers.value('location') != null)
        'location: ${LogSanitizer.urlString(headers.value('location'))}',
      if (headers.value('server') != null) 'server: ${headers.value('server')}',
      'proxy cookies sent: ${LogSanitizer.cookieNames(proxyCookies().keys)}',
      if (proxyRefused) 'refused by the SSO proxy',
    ];
    appLog.warn(
      'api',
      '$request → ${response.statusCode}${_duration(options)} (${details.join(', ')})',
    );
  }

  static bool _isTwoFactorRequired(Object? data) =>
      data is Map && data['twoFactorRequired'] == true;

  /// An answer of an SSO proxy in front of the server, not of ProjectForge: a 401 without the json of the server
  /// (RestAuthenticationUtils.sendError), or a redirect (to the proxy's login; the api itself never redirects).
  ///
  /// Told by the content type, not by the parsed data: a download gets the json of a 401 as bytes.
  static bool isProxyRefusal(Response<dynamic>? response) {
    final status = response?.statusCode;
    if (status == null) return false;
    if (status >= 300 && status < 400) return true;
    final contentType = response!.headers.value(Headers.contentTypeHeader);
    return status == 401 && !(contentType?.contains('json') ?? false);
  }

  /// The server's answer as [ApiException]: the validation errors of a 406, the message of the json error body,
  /// or no status at all if the server wasn't reached.
  static ApiException toApiException(DioException e) {
    final response = e.response;
    if (response == null) {
      return ApiException(e.message ?? e.type.name);
    }
    if (isProxyRefusal(response)) {
      return ApiException(
        'HTTP ${response.statusCode}',
        statusCode: response.statusCode,
        messageKey: proxyRefusedKey,
      );
    }
    final data = response.data;
    if (data is Map<String, dynamic>) {
      final errors = (data['validationErrors'] as List<dynamic>? ?? [])
          .map((json) => ValidationError.fromJson(json as Map<String, dynamic>))
          .toList();
      final message = errors
          .map((it) => it.message)
          .whereType<String>()
          .join('\n');
      return ApiException(
        message.isNotEmpty
            ? message
            : (data['message'] as String? ?? 'HTTP ${response.statusCode}'),
        statusCode: response.statusCode,
        validationErrors: errors,
      );
    }
    return ApiException(
      'HTTP ${response.statusCode}',
      statusCode: response.statusCode,
    );
  }
}
