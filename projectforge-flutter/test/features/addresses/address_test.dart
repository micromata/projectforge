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

import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/features/addresses/address.dart';

Address address(Map<String, dynamic> json) =>
    Address.fromJson({'id': 1, 'lastUpdate': 10, ...json});

void main() {
  test('the list is sorted by last name, umlauts as their letter', () {
    final book = AddressBook(1, [
      address({'id': 1, 'name': 'Zander'}),
      address({'id': 2, 'name': 'Ärger', 'firstName': 'Anna'}),
      address({'id': 3, 'name': 'Abel'}),
      address({'id': 4, 'name': '1Plus'}),
      address({'id': 5, 'organization': 'Acme'}),
    ]);
    expect(book.addresses.map((it) => it.id), [3, 5, 2, 1, 4]);
  });

  test('the section is the first letter, "#" for anything else', () {
    expect(address({'name': 'Ärger'}).sectionLetter, 'A');
    expect(address({'name': 'özil'}).sectionLetter, 'O');
    expect(address({'name': '42'}).sectionLetter, '#');
    expect(address({}).sectionLetter, '#');
    expect(address({'organization': 'Acme'}).sectionLetter, 'A');
  });

  test('the name and the initials', () {
    final a = address({
      'title': 'Dr.',
      'firstName': 'Jane',
      'name': 'Doe',
      'organization': 'Acme',
    });
    expect(a.displayName, 'Dr. Jane Doe');
    expect(a.initials, 'JD');
    expect(a.subtitle, 'Acme');
    final org = address({'organization': 'Acme'});
    expect(org.displayName, 'Acme');
    expect(org.subtitle, isNull);
  });

  test('the search finds words in any order, without umlauts, and numbers', () {
    final a = address({
      'firstName': 'Jürgen',
      'name': 'Müller',
      'organization': 'Acme',
      'email': 'jm@example.org',
      'mobilePhone': '+49 (170) 123-456',
    });
    expect(a.matches(''), isTrue);
    expect(a.matches('muller jurgen'), isTrue);
    expect(a.matches('acme müller'), isTrue);
    expect(a.matches('example.org'), isTrue);
    expect(a.matches('170123'), isTrue);
    expect(a.matches('999'), isFalse);
    expect(a.matches('schmidt'), isFalse);
  });

  test('the phone numbers keep their kind, only mobile ones take messages', () {
    final a = address({
      'businessPhone': '111',
      'mobilePhone': '222',
      'fax': ' ',
    });
    expect(a.phones.map((it) => (it.number, it.mobile)), [
      ('111', false),
      ('222', true),
    ]);
  });

  test('a sync replaces changed addresses and drops the removed ones', () {
    final stored = AddressBook(1, [
      address({'id': 1, 'name': 'Alt'}),
      address({'id': 2, 'name': 'Weg'}),
      address({'id': 3, 'name': 'Bleibt'}),
    ]);
    final merged = stored.merge(
      5,
      [1, 3, 4],
      [
        address({'id': 1, 'name': 'Neu'}),
        address({'id': 4, 'name': 'Dazu'}),
      ],
    )!;
    expect(merged.timestamp, 5);
    expect(merged.addresses.map((it) => it.name), ['Bleibt', 'Dazu', 'Neu']);
  });

  test('an address that became visible unchanged needs a full load', () {
    final stored = AddressBook(1, [
      address({'id': 1}),
    ]);
    expect(stored.merge(5, [1, 9], const []), isNull);
  });

  test('the favorites survive storing the book', () {
    final book = AddressBook(
      1,
      [
        address({'id': 1}),
        address({'id': 2}),
      ],
      favoriteIds: {2},
    );
    final stored = AddressBook.fromJson(book.toJson());
    expect(stored.favorites.map((it) => it.id), [2]);
  });
}
