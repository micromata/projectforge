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

/// A field of a business card the user picks and edits (all but card name and photo), with the key of its label
/// (exported by the prefix `businessCard.` of GenerateFlutterI18nMessagesMain). Mirrors BUSINESS_CARD_FIELDS of
/// projectforge-next: the private fields are unticked when the data is taken over from an address.
class BusinessCardField {
  const BusinessCardField(
    this.name,
    this.labelKey, {
    this.private = false,
    this.multiline = false,
  });

  final String name;
  final String labelKey;
  final bool private;
  final bool multiline;
}

const businessCardFields = [
  BusinessCardField('title', 'businessCard.title'),
  BusinessCardField('firstName', 'businessCard.firstName'),
  BusinessCardField('lastName', 'businessCard.lastName'),
  BusinessCardField('organization', 'businessCard.organization'),
  BusinessCardField('division', 'businessCard.division'),
  BusinessCardField('positionText', 'businessCard.positionText'),
  BusinessCardField('email', 'businessCard.email'),
  BusinessCardField('businessPhone', 'businessCard.businessPhone'),
  BusinessCardField('mobilePhone', 'businessCard.mobilePhone'),
  BusinessCardField('fax', 'businessCard.fax'),
  BusinessCardField('website', 'businessCard.website'),
  BusinessCardField('street', 'businessCard.street'),
  BusinessCardField('zipCode', 'businessCard.zipCode'),
  BusinessCardField('city', 'businessCard.city'),
  BusinessCardField('country', 'businessCard.country'),
  BusinessCardField('comment', 'businessCard.comment', multiline: true),
  BusinessCardField('privateEmail', 'businessCard.privateEmail', private: true),
  BusinessCardField('privatePhone', 'businessCard.privatePhone', private: true),
  BusinessCardField(
    'privateMobilePhone',
    'businessCard.privateMobilePhone',
    private: true,
  ),
  BusinessCardField(
    'privateStreet',
    'businessCard.privateStreet',
    private: true,
  ),
  BusinessCardField(
    'privateZipCode',
    'businessCard.privateZipCode',
    private: true,
  ),
  BusinessCardField('privateCity', 'businessCard.privateCity', private: true),
  BusinessCardField(
    'privateCountry',
    'businessCard.privateCountry',
    private: true,
  ),
  BusinessCardField('birthday', 'businessCard.birthday', private: true),
];

/// From this length on the QR code gets dense (see BusinessCardRest.Preview.vCardLength).
const denseVCardLength = 1000;
