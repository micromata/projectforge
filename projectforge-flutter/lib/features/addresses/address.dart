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

/// A postal address of an [Address] (business, postal or private).
class PostalAddress {
  PostalAddress.fromJson(Map<String, dynamic> json)
    : addressText = json['addressText'] as String?,
      addressText2 = json['addressText2'] as String?,
      zipCode = json['zipCode'] as String?,
      city = json['city'] as String?,
      state = json['state'] as String?,
      country = json['country'] as String?;

  final String? addressText;
  final String? addressText2;
  final String? zipCode;
  final String? city;
  final String? state;
  final String? country;

  /// The lines as written on an envelope.
  List<String> get lines => [
    addressText,
    addressText2,
    [zipCode, city].where(_filled).join(' '),
    [state, country].where(_filled).join(', '),
  ].whereType<String>().where(_filled).toList();

  /// For the search of a maps app.
  String get oneLine => lines.join(', ');
}

/// One phone number of an address with the key of its kind.
class PhoneNumber {
  const PhoneNumber(this.labelKey, this.number, {required this.mobile});

  /// The i18n key of the kind (business, mobile, …).
  final String labelKey;
  final String number;

  /// A text message can be sent to it.
  final bool mobile;
}

bool _filled(String? value) => value != null && value.trim().isNotEmpty;

/// An address as the server's `AddressEntityRest.AppAddress` delivers it for the address book of the app.
class Address {
  Address.fromJson(Map<String, dynamic> json)
    : json = Map<String, dynamic>.from(json),
      id = json['id'] as int,
      title = json['title'] as String?,
      firstName = json['firstName'] as String?,
      name = json['name'] as String?,
      organization = json['organization'] as String?,
      division = json['division'] as String?,
      positionText = json['positionText'] as String?,
      email = json['email'] as String?,
      privateEmail = json['privateEmail'] as String?,
      website = json['website'] as String?,
      birthday = json['birthday'] as String?,
      comment = json['comment'] as String?,
      hasImage = json['hasImage'] == true,
      writeAccess = json['writeAccess'] == true,
      lastUpdate = json['lastUpdate'] as int? ?? 0,
      business = _postal(json['business']),
      postal = _postal(json['postal']),
      private = _postal(json['private']),
      phones = [
        for (final (field, key, mobile) in _phoneFields)
          if (_filled(json[field] as String?))
            PhoneNumber(key, (json[field] as String).trim(), mobile: mobile),
      ];

  /// Kept as it came, to store it again.
  final Map<String, dynamic> json;

  final int id;
  final String? title;
  final String? firstName;
  final String? name;
  final String? organization;
  final String? division;
  final String? positionText;
  final String? email;
  final String? privateEmail;
  final String? website;

  /// ISO date `yyyy-MM-dd`.
  final String? birthday;
  final String? comment;
  final bool hasImage;
  final bool writeAccess;

  /// The version of the address and its image: the image on the device is valid for the same value.
  final int lastUpdate;
  final PostalAddress? business;
  final PostalAddress? postal;
  final PostalAddress? private;
  final List<PhoneNumber> phones;

  static const _phoneFields = [
    ('businessPhone', 'address.phoneType.business', false),
    ('mobilePhone', 'address.phoneType.mobile', true),
    ('privatePhone', 'address.phoneType.private', false),
    ('privateMobilePhone', 'address.phoneType.privateMobile', true),
    ('fax', 'address.phoneType.fax', false),
  ];

  /// The same address in the same version.
  @override
  bool operator ==(Object other) =>
      other is Address && other.id == id && other.lastUpdate == lastUpdate;

  @override
  int get hashCode => Object.hash(id, lastUpdate);

  static PostalAddress? _postal(Object? json) => json == null
      ? null
      : PostalAddress.fromJson(json as Map<String, dynamic>);

  /// The e-mail addresses, the private one marked.
  List<({String address, bool private})> get emails => [
    if (_filled(email)) (address: email!.trim(), private: false),
    if (_filled(privateEmail)) (address: privateEmail!.trim(), private: true),
  ];

  /// "Doe, Jane" by which the list is sorted: the last name, then the first name; without them the organization.
  String get sortKey {
    final person = [name, firstName].where(_filled).join(', ');
    return person.isNotEmpty ? person : (organization ?? '').trim();
  }

  /// "Dr. Jane Doe", or the organization if the address has no person.
  String get displayName {
    final person = [title, firstName, name].where(_filled).join(' ');
    return person.isNotEmpty ? person : (organization ?? '').trim();
  }

  /// "Doe, Jane" for the list, so the sorting is visible; the organization if the address has no person.
  String get listName => sortKey;

  /// The second line of a list row: the organization of a person.
  String? get subtitle {
    final person = [name, firstName].any(_filled);
    return person && _filled(organization) ? organization!.trim() : null;
  }

  /// Two letters for the avatar.
  String get initials {
    final person = [
      firstName,
      name,
    ].whereType<String>().where(_filled).toList();
    final source = person.isNotEmpty ? person : [organization ?? ''];
    final letters = [
      for (final part in source)
        if (part.trim().isNotEmpty) part.trim().substring(0, 1).toUpperCase(),
    ];
    return letters.take(2).join();
  }

  /// "A" to "Z" for the section of the list, "#" for everything else.
  String get sectionLetter {
    final key = sortKey;
    if (key.isEmpty) return '#';
    final letter = foldLetter(key.substring(0, 1));
    return RegExp(r'^[A-Z]$').hasMatch(letter) ? letter : '#';
  }

  /// Every word of [query] is part of the name, the organization, an e-mail or a city; digits are also looked for
  /// in the phone numbers (without spaces and signs).
  bool matches(String query) {
    final words = query
        .toLowerCase()
        .split(RegExp(r'\s+'))
        .where((it) => it.isNotEmpty);
    if (words.isEmpty) return true;
    final text = foldText(
      [
        title,
        firstName,
        name,
        organization,
        division,
        positionText,
        email,
        privateEmail,
        business?.city,
        postal?.city,
        private?.city,
      ].whereType<String>().join(' '),
    );
    final digits = phones
        .map((it) => it.number.replaceAll(RegExp(r'[^0-9]'), ''))
        .join(' ');
    return words.every((word) {
      if (text.contains(foldText(word))) return true;
      final wordDigits = word.replaceAll(RegExp(r'[^0-9]'), '');
      return wordDigits.isNotEmpty &&
          wordDigits.length == word.replaceAll('+', '').length &&
          digits.contains(wordDigits);
    });
  }
}

const _folds = {
  'ä': 'a',
  'ö': 'o',
  'ü': 'u',
  'ß': 's',
  'á': 'a',
  'à': 'a',
  'â': 'a',
  'é': 'e',
  'è': 'e',
  'ê': 'e',
  'ë': 'e',
  'í': 'i',
  'ì': 'i',
  'î': 'i',
  'ï': 'i',
  'ó': 'o',
  'ò': 'o',
  'ô': 'o',
  'ú': 'u',
  'ù': 'u',
  'û': 'u',
  'ç': 'c',
  'ñ': 'n',
  'å': 'a',
  'ø': 'o',
  'ł': 'l',
  'š': 's',
  'ž': 'z',
  'č': 'c',
  'ř': 'r',
  'ő': 'o',
  'ű': 'u',
};

/// Lower case without umlauts and accents, so "Müller" is found by "muller".
String foldText(String text) {
  final lower = text.toLowerCase();
  final buffer = StringBuffer();
  for (final rune in lower.runes) {
    final char = String.fromCharCode(rune);
    buffer.write(_folds[char] ?? char);
  }
  return buffer.toString();
}

/// The upper case letter of [char] without accent ("Ä" → "A").
String foldLetter(String char) => foldText(char).toUpperCase();

/// The address book stored on the device: the addresses sorted for the list, and the time of the server up to
/// which they are up to date.
class AddressBook {
  AddressBook(
    this.timestamp,
    List<Address> addresses, {
    this.favoriteIds = const {},
    this.fromCache = false,
  }) : addresses = (List<Address>.of(addresses)..sort(_compare));

  AddressBook.fromJson(Map<String, dynamic> json, {bool fromCache = false})
    : this(
        json['timestamp'] as int? ?? 0,
        [
          for (final it in json['addresses'] as List<dynamic>? ?? [])
            Address.fromJson(it as Map<String, dynamic>),
        ],
        favoriteIds: {
          ...(json['favoriteIds'] as List<dynamic>? ?? []).cast<int>(),
        },
        fromCache: fromCache,
      );

  /// The `modifiedSince` of the next request.
  final int timestamp;
  final List<Address> addresses;

  /// The ids of the favorites of the user.
  final Set<int> favoriteIds;

  /// The addresses marked as favorite.
  List<Address> get favorites => [
    for (final it in addresses)
      if (favoriteIds.contains(it.id)) it,
  ];

  /// The server wasn't reachable: the addresses are the stored ones.
  final bool fromCache;

  Map<String, dynamic> toJson() => {
    'timestamp': timestamp,
    'favoriteIds': favoriteIds.toList(),
    'addresses': [for (final it in addresses) it.json],
  };

  AddressBook asFromCache() => AddressBook(
    timestamp,
    addresses,
    favoriteIds: favoriteIds,
    fromCache: true,
  );

  /// The book after a sync: the changed addresses replace the stored ones, the ones not in [ids] are gone. Null if
  /// [ids] holds addresses this book doesn't have and that aren't among [changed] (a newly visible address that
  /// wasn't modified): then everything has to be loaded.
  AddressBook? merge(
    int timestamp,
    List<int> ids,
    List<Address> changed, {
    Iterable<int> favoriteIds = const [],
  }) {
    final byId = {for (final it in addresses) it.id: it};
    for (final it in changed) {
      byId[it.id] = it;
    }
    final keep = ids.toSet();
    if (keep.any((id) => !byId.containsKey(id))) return null;
    byId.removeWhere((id, _) => !keep.contains(id));
    return AddressBook(
      timestamp,
      byId.values.toList(),
      favoriteIds: {...favoriteIds},
    );
  }

  static int _compare(Address a, Address b) {
    // Addresses without a letter (digits, signs) at the end, as in the list of Apple.
    final aLast = a.sectionLetter == '#';
    final bLast = b.sectionLetter == '#';
    if (aLast != bLast) return aLast ? 1 : -1;
    return foldText(a.sortKey).compareTo(foldText(b.sortKey));
  }
}
