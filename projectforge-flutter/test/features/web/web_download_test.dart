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
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/features/web/web_download.dart';
import 'package:projectforge_app/features/web/web_navigation.dart';

import '../../core/fake_adapter.dart';

void main() {
  final fileUrl = Uri.parse(
    'https://pf.example.org/rs/chat/conversation/1/file/abc?download=true',
  );

  test('a Wallet pass is recognized by its type or its extension', () {
    expect(
      WebDownload.isWalletPass(mimeType: 'application/vnd.apple.pkpass'),
      isTrue,
    );
    expect(WebDownload.isWalletPass(fileName: 'Jane Doe.pkpass'), isTrue);
    expect(
      WebDownload.isWalletPass(mimeType: 'text/vcard', fileName: 'a.vcf'),
      isFalse,
    );
    expect(WebDownload.isWalletPass(), isFalse);
  });

  group('fileName', () {
    test(
      'Content-Disposition first, the RFC 5987 name before the plain one',
      () {
        expect(
          WebDownload.fileName(
            contentDisposition: 'attachment; filename="Bericht.pdf"; filename*=UTF-8\'\'Bericht%20%C3%A4.pdf',
            suggestedFilename: 'other.pdf',
            url: fileUrl,
          ),
          'Bericht ä.pdf',
        );
        expect(
          WebDownload.fileName(
            contentDisposition: 'attachment; filename="Report.pdf"',
            url: fileUrl,
          ),
          'Report.pdf',
        );
      },
    );

    test('then the suggested name, the url and a default', () {
      expect(
        WebDownload.fileName(suggestedFilename: 'a.txt', url: fileUrl),
        'a.txt',
      );
      expect(WebDownload.fileName(url: fileUrl), 'abc');
      expect(
        WebDownload.fileName(url: Uri.parse('https://pf.example.org/')),
        'download',
      );
    });

    test('no path leaves the download directory', () {
      expect(
        WebDownload.fileName(
          contentDisposition: 'attachment; filename="../../etc/passwd"',
          url: fileUrl,
        ),
        'passwd',
      );
      expect(
        WebDownload.fileName(
          contentDisposition: 'attachment; filename=".."',
          suggestedFilename: r'..\x.txt',
          url: fileUrl,
        ),
        'x.txt',
      );
    });
  });

  group('load', () {
    late FakeAdapter adapter;
    late Directory directory;

    WebDownload download() => WebDownload(
      navigation: WebNavigation(Uri.parse('https://pf.example.org')),
      cookieHeader: (_) async => 'JSESSIONID=web',
      dio: Dio()..httpClientAdapter = adapter,
      directory: () async => directory,
    );

    setUp(() async {
      adapter = FakeAdapter((_) => (status: 200, body: 'content'));
      directory = await Directory.systemTemp.createTemp('pf_download_test');
    });

    tearDown(() => directory.delete(recursive: true));

    test(
      'loads with the session cookie of the WebView, not the app token',
      () async {
        final file = await download().load(fileUrl, suggestedFilename: 'f.txt');
        final headers = adapter.requests.single.headers;
        expect(headers['Cookie'], 'JSESSIONID=web');
        expect(headers.containsKey('Authorization'), isFalse);
        expect(adapter.requests.single.followRedirects, isFalse);
        expect(file.path, '${directory.path}/f.txt');
        expect(await file.readAsString(), '"content"');
      },
    );

    test('urls of another server are refused', () async {
      for (final url in [
        'https://other.example.org/file',
        'http://pf.example.org/file',
        'data:text/plain,x',
      ]) {
        await expectLater(
          download().load(Uri.parse(url)),
          throwsA(isA<WebDownloadRefused>()),
          reason: url,
        );
      }
      expect(adapter.requests, isEmpty);
    });
  });
}
