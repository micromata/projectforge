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

import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/features/notifications/notification_text.dart';

void main() {
  test('html becomes plain text', () {
    expect(
      htmlToText(
        '<p>Hello <b>you</b> &amp; &lt;all&gt;</p><ul><li>one</li><li>two</li></ul>',
      ),
      'Hello you & <all>\n\n• one\n• two',
    );
    expect(htmlToText(''), '');
    expect(htmlToText('a<br/>b'), 'a\nb');
  });

  test('links are paths of the own server', () {
    expect(webPathOfLink('/timesheet'), '/next/timesheet');
    expect(webPathOfLink('timesheet/5'), '/next/timesheet/5');
    expect(
      webPathOfLink('next/monthlyEmployeeReport'),
      '/next/monthlyEmployeeReport',
    );
    expect(webPathOfLink('react/calendar'), '/react/calendar');
    expect(webPathOfLink('/chat/5?message=7'), '/next/chat/5?message=7');
    expect(webPathOfLink(null), isNull);
    expect(webPathOfLink(' '), isNull);
  });

  test('other hosts are never followed', () {
    expect(webPathOfLink('https://evil.example.com/x'), isNull);
    expect(webPathOfLink('//evil.example.com/x'), isNull);
    expect(webPathOfLink('javascript:alert(1)'), isNull);
  });

  test('chat pages', () {
    expect(isChatPath('/next/chat'), isTrue);
    expect(isChatPath('/next/chat/5?message=7'), isTrue);
    expect(isChatPath('/next/chatty'), isFalse);
    expect(isChatPath('/next/timesheet'), isFalse);
  });
}
