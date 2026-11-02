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

/// Keeps secrets out of the client log ([AppLog]): no token, cookie value, code, verifier or password is written.
///
/// The log points pass urls through [url] and cookies through [cookieNames]; [text] is applied to every message as
/// a safety net for what slips through (e.g. the text of an exception).
class LogSanitizer {
  LogSanitizer._();

  static const mask = '***';

  /// `Authorization: PFApp …`, `Bearer …`.
  static final _authorization = RegExp(
    r'\b(PFApp|Bearer|Basic)\s+[^\s,;]+',
    caseSensitive: false,
  );

  /// A JSON Web Token (three base64url parts).
  static final _jwt = RegExp(
    r'\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*',
  );

  /// The names of secret parameters, queries, cookies and json fields.
  static const _secretName =
      r'(?:[A-Za-z_]*(?:token|password|passwd|secret|cookie)[A-Za-z0-9_]*|code|codeVerifier|code_verifier|verifier|challenge|state|otp|nonce|ticket|api_?key|authentik[A-Za-z0-9_]*|JSESSIONID|SESSION)';

  /// `name=value` of a query or cookie.
  static final _secretParameter = RegExp(
    '\\b($_secretName)=([^\\s&;,]+)',
    caseSensitive: false,
  );

  /// `"name": "value"` of json.
  static final _secretJson = RegExp(
    '"($_secretName)"(\\s*:\\s*)("[^"]*"|[^\\s,}]+)',
    caseSensitive: false,
  );

  /// A mail address (personal data).
  static final _mail = RegExp(r'[\w.+-]+@[\w-]+(\.[\w-]+)+');

  /// The cookies of a `Cookie` header (`a=1; b=2`): `a=***; b=***`.
  static final _cookiePair = RegExp(r'(^|;\s*)([^=;\s]+)=[^;]*');

  /// [message] without anything that looks like a secret.
  static String text(String message) => message
      .replaceAllMapped(_authorization, (m) => '${m[1]} $mask')
      .replaceAll(_jwt, mask)
      .replaceAll(_mail, mask)
      .replaceAllMapped(_secretParameter, (m) => '${m[1]}=$mask')
      .replaceAllMapped(_secretJson, (m) => '"${m[1]}"${m[2]}$mask');

  /// Scheme, host, port and path of [url]; the values of its query are masked (their names show what arrived), the
  /// fragment is dropped.
  static String url(Uri? url) {
    if (url == null) return '-';
    if (!_personalSchemes.contains(url.scheme)) {
      final query = url.queryParametersAll.keys.map((it) => '$it=$mask');
      final base = url
          .replace(userInfo: '', query: '', fragment: '')
          .toString();
      final plain = base.replaceAll(RegExp(r'[?#]+$'), '');
      return query.isEmpty ? plain : '$plain?${query.join('&')}';
    }
    return '${url.scheme}:$mask';
  }

  /// Urls that are personal data as a whole (the address, the number): only their scheme is logged.
  static const _personalSchemes = {'mailto', 'tel', 'sms'};

  /// As [url], for a string that may not be an url.
  static String urlString(String? value) {
    if (value == null) return '-';
    final uri = Uri.tryParse(value);
    return uri == null ? text(value) : url(uri);
  }

  /// The names of [cookies] (name to value), never the values.
  static String cookieNames(Iterable<String> cookies) =>
      cookies.isEmpty ? '-' : (cookies.toList()..sort()).join(', ');

  /// The names of the cookies of a `Cookie` header.
  static String cookieHeader(String? header) => header == null
      ? '-'
      : header.replaceAllMapped(_cookiePair, (m) => '${m[1]}${m[2]}=$mask');
}
