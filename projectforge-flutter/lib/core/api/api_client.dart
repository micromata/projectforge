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

import 'api_exception.dart';

/// The calls of the app to its ProjectForge server (`/rs`, authenticated by the app token).
///
/// - Every request carries `Authorization: PFApp <token>`: no session, no cookies, hence no CSRF token.
/// - `X-PF-Frontend: next` selects the lean list rows projectforge-next gets.
/// - A 401 means the token is gone (logged out in the web, expired, password changed): [onUnauthorized].
/// - A 403 with `twoFactorRequired` means the second factor of the app's login is too old for this url: the app
///   logs in again in the browser ([onReauthenticate]) and repeats the request once with the new token.
class ApiClient {
  ApiClient({
    required this.serverUrl,
    required this.token,
    required this.appVersion,
    required this.onUnauthorized,
    required this.onReauthenticate,
    Dio? dio,
  }) : dio = dio ?? Dio() {
    this.dio.options
      ..baseUrl = serverUrl
      ..connectTimeout = const Duration(seconds: 15)
      ..receiveTimeout = const Duration(seconds: 30);
    this.dio.interceptors.add(
      InterceptorsWrapper(onRequest: _onRequest, onError: _onError),
    );
  }

  static const _retriedKey = 'pfRetried';

  final String serverUrl;
  final String appVersion;
  final Dio dio;
  final void Function() onUnauthorized;

  /// The current token of the app, read for every request: a new login replaces it without a new client.
  final String? Function() token;

  /// Does a new login in the browser; answers false if the user cancelled.
  final Future<bool> Function() onReauthenticate;

  Future<T> get<T>(String path, {Map<String, dynamic>? query}) =>
      _call(() => dio.get<T>(path, queryParameters: query));

  Future<T> post<T>(String path, {Object? data, Map<String, dynamic>? query}) =>
      _call(() => dio.post<T>(path, data: data, queryParameters: query));

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
    handler.next(options);
  }

  Future<void> _onError(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final response = error.response;
    if (response?.statusCode == 401) {
      onUnauthorized();
    } else if (response?.statusCode == 403 &&
        _isTwoFactorRequired(response?.data) &&
        error.requestOptions.extra[_retriedKey] != true) {
      if (await onReauthenticate()) {
        final options = error.requestOptions..extra[_retriedKey] = true;
        try {
          return handler.resolve(await dio.fetch(options));
        } on DioException catch (retryError) {
          return handler.next(retryError);
        }
      }
    }
    handler.next(error);
  }

  static bool _isTwoFactorRequired(Object? data) =>
      data is Map && data['twoFactorRequired'] == true;

  /// The server's answer as [ApiException]: the validation errors of a 406, the message of the json error body,
  /// or no status at all if the server wasn't reached.
  static ApiException toApiException(DioException e) {
    final response = e.response;
    if (response == null) {
      return ApiException(e.message ?? e.type.name);
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
