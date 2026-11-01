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
import 'package:path_provider/path_provider.dart';

import 'web_navigation.dart';

/// A download started by the pages in the WebView (a chat file, a data transfer file, …), which a WebView doesn't do
/// itself: the app loads the file and offers it in the share sheet (which also saves it to the files).
///
/// The request carries the WebView's session cookie, never the app token: the url is picked by the page, and only
/// urls of the own server are loaded. Redirects aren't followed, so the cookie doesn't go anywhere else.
class WebDownload {
  WebDownload({
    required this.navigation,
    required this.cookieHeader,
    Dio? dio,
    Future<Directory> Function()? directory,
  }) : _dio = dio ?? Dio(),
       _directory = directory ?? getTemporaryDirectory {
    _dio.options
      ..connectTimeout = const Duration(seconds: 15)
      ..receiveTimeout = const Duration(minutes: 5)
      ..followRedirects = false;
  }

  final WebNavigation navigation;

  /// The `Cookie` header of the WebView for a url (`WebCookies.cookieHeader`).
  final Future<String?> Function(Uri url) cookieHeader;
  final Dio _dio;
  final Future<Directory> Function() _directory;

  /// Loads [url] into a temporary file, named by the server's `Content-Disposition`, else by
  /// [suggestedFilename] of the WebView, else by the url.
  ///
  /// Throws [WebDownloadRefused] for a url of another server, [DioException] if loading failed.
  Future<File> load(Uri url, {String? suggestedFilename}) async {
    if (!(url.scheme == 'http' || url.scheme == 'https') ||
        !navigation.isSameOrigin(url)) {
      throw WebDownloadRefused(url);
    }
    final cookie = await cookieHeader(url);
    final response = await _dio.getUri<List<int>>(
      url,
      options: Options(
        responseType: ResponseType.bytes,
        headers: {'Cookie': ?cookie},
      ),
    );
    final name = fileName(
      contentDisposition: response.headers.value('content-disposition'),
      suggestedFilename: suggestedFilename,
      url: url,
    );
    final dir = Directory('${(await _directory()).path}/downloads');
    await dir.create(recursive: true);
    final file = File('${dir.path}/$name');
    await file.writeAsBytes(response.data ?? const [], flush: true);
    return file;
  }

  /// The name of the file: `filename*` (RFC 5987) or `filename` of [contentDisposition], [suggestedFilename], the
  /// last segment of [url], or `download`. Without any path, so it stays in the download directory.
  static String fileName({
    String? contentDisposition,
    String? suggestedFilename,
    required Uri url,
  }) {
    final candidates = [
      _dispositionName(contentDisposition),
      suggestedFilename,
      url.pathSegments.isEmpty ? null : url.pathSegments.last,
    ];
    for (final candidate in candidates) {
      final name = _sanitize(candidate);
      if (name != null) return name;
    }
    return 'download';
  }

  static String? _dispositionName(String? header) {
    if (header == null) return null;
    final extended = RegExp(
      r"""filename\*\s*=\s*([^']*)'[^']*'([^;]+)""",
      caseSensitive: false,
    ).firstMatch(header);
    if (extended != null) {
      try {
        return Uri.decodeComponent(extended.group(2)!.trim());
      } on ArgumentError {
        // Falls back to the plain filename.
      }
    }
    final plain = RegExp(
      r'''filename\s*=\s*(?:"([^"]*)"|([^;]+))''',
      caseSensitive: false,
    ).firstMatch(header);
    return plain == null ? null : (plain.group(1) ?? plain.group(2))?.trim();
  }

  static String? _sanitize(String? name) {
    if (name == null) return null;
    final base = name.split(RegExp(r'[/\\]')).last;
    final clean = base
        .replaceAll(RegExp(r'[\x00-\x1f\x7f:*?"<>|]'), '_')
        .trim();
    if (clean.isEmpty || clean == '.' || clean == '..') return null;
    return clean;
  }
}

/// The page asked for a download of another server.
class WebDownloadRefused implements Exception {
  WebDownloadRefused(this.url);

  final Uri url;

  @override
  String toString() => 'Download of another server refused: $url';
}
