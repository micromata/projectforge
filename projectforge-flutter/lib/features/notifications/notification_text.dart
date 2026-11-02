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

/// The text of a notification (sanitized html) as plain text: paragraphs and lists become lines, tags go, the common
/// entities are resolved.
String htmlToText(String html) {
  var text = html
      .replaceAll(RegExp(r'<\s*br\s*/?\s*>', caseSensitive: false), '\n')
      .replaceAll(RegExp(r'<\s*li[^>]*>', caseSensitive: false), '\n• ')
      .replaceAll(
        RegExp(r'<\s*/\s*(p|div|ul|ol|h[1-6]|tr)\s*>', caseSensitive: false),
        '\n',
      )
      .replaceAll(RegExp(r'<[^>]*>'), '');
  const entities = {
    '&nbsp;': ' ',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&#39;': "'",
    '&amp;': '&', // Last: "&amp;lt;" is the text "&lt;".
  };
  entities.forEach((entity, value) => text = text.replaceAll(entity, value));
  return text
      .replaceAll(RegExp(r'[ \t]+\n'), '\n')
      .replaceAll(RegExp(r'\n{3,}'), '\n\n')
      .trim();
}

/// The page of the app a [link] of a notification opens, as a path of the server: the links are paths of the next
/// app (`/timesheet`) or of the legacy app (`react/...`); other servers (absolute urls) aren't followed.
String? webPathOfLink(String? link) {
  final value = link?.trim();
  if (value == null || value.isEmpty) return null;
  if (RegExp(r'^[a-zA-Z][a-zA-Z0-9+.-]*:').hasMatch(value) ||
      value.startsWith('//')) {
    return null;
  }
  final relative = value.startsWith('/') ? value.substring(1) : value;
  if (relative.startsWith('next/') ||
      relative.startsWith('react/') ||
      relative == 'next') {
    return '/$relative';
  }
  return '/next/$relative';
}

/// True if [path] (see [webPathOfLink]) is a page of the chat, which has a tab of its own.
bool isChatPath(String path) =>
    path == '/next/chat' ||
    path.startsWith('/next/chat/') ||
    path.startsWith('/next/chat?');
