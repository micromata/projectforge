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
import 'dart:typed_data';

/// A business card as the server's BusinessCard serializes it. Kept as the json map it came as, so a field this
/// version of the app doesn't know survives an edit.
class BusinessCard {
  BusinessCard(Map<String, dynamic> json)
    : json = Map<String, dynamic>.from(json);

  final Map<String, dynamic> json;

  String? get id => json['id'] as String?;
  String? get cardName => json['cardName'] as String?;
  int? get addressId => json['addressId'] as int?;
  bool get includePhoto => json['includePhoto'] == true;

  String? operator [](String field) => json[field]?.toString();

  /// "Dr. Jane Doe", for the list.
  String get displayName => [
    this['title'],
    this['firstName'],
    this['lastName'],
  ].whereType<String>().where((it) => it.isNotEmpty).join(' ');

  BusinessCard copy() => BusinessCard(json);
}

/// Mirror of BusinessCardRest.Preview.
class BusinessCardPreview {
  BusinessCardPreview.fromJson(Map<String, dynamic> json)
    : qrCodeUrl = json['qrCodeUrl'] as String? ?? '',
      vCardLength = json['vCardLength'] as int? ?? 0,
      withPhoto = json['withPhoto'] == true,
      photoAllowed = json['photoAllowed'] == true;

  /// The QR code with the vCard as data url (`data:image/png;base64,…`).
  final String qrCodeUrl;
  final int vCardLength;
  final bool withPhoto;
  final bool photoAllowed;

  Uint8List? get qrCodePng {
    final comma = qrCodeUrl.indexOf(',');
    if (comma < 0) return null;
    return base64Decode(qrCodeUrl.substring(comma + 1));
  }
}

/// Mirror of BusinessCardRest.CardInfo.
class BusinessCardInfo {
  BusinessCardInfo.fromJson(Map<String, dynamic> json)
    : card = BusinessCard(json['card'] as Map<String, dynamic>),
      preview = BusinessCardPreview.fromJson(
        json['preview'] as Map<String, dynamic>,
      );

  final BusinessCard card;
  final BusinessCardPreview preview;
}

/// Mirror of BusinessCardRest.InitData, plus whether it comes from the cache (server not reachable).
class BusinessCards {
  BusinessCards.fromJson(Map<String, dynamic> json, {this.fromCache = false})
    : cards = (json['cards'] as List<dynamic>? ?? [])
          .map((it) => BusinessCardInfo.fromJson(it as Map<String, dynamic>))
          .toList(),
      suggestion = json['suggestion'] == null
          ? null
          : BusinessCard(json['suggestion'] as Map<String, dynamic>),
      maxCards = json['maxCards'] as int? ?? 20,
      walletConfigured = json['walletConfigured'] == true;

  final List<BusinessCardInfo> cards;

  /// A new card from the address that is most likely the user's own one.
  final BusinessCard? suggestion;
  final int maxCards;

  /// The server can build the Apple Wallet pass of a card.
  final bool walletConfigured;
  final bool fromCache;
}
