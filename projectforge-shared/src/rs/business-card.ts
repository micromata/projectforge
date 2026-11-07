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

// The own business cards of the user (BusinessCardRest, GET /rs/myBusinessCards, /vcf, /pkpass). See src/rs/app-auth.ts
// for @server.

/** The cards, created and edited in the web pages. @server org.projectforge.rest.businesscard.BusinessCardRest$InitData */
export interface BusinessCardsData {
  cards: BusinessCardInfo[];
  /** The server can build the Apple Wallet pass of a card. */
  walletConfigured?: boolean;
}

/** @server org.projectforge.rest.businesscard.BusinessCardRest$CardInfo */
export interface BusinessCardInfo {
  card: BusinessCard;
  preview: BusinessCardPreview;
}

/** @server org.projectforge.rest.businesscard.BusinessCardRest$Preview */
export interface BusinessCardPreview {
  /** The QR code with the vCard as data url (`data:image/png;base64,…`). */
  qrCodeUrl: string;
}

/** The fields of a card the app shows. @server org.projectforge.business.address.businesscard.BusinessCard */
export interface BusinessCard {
  id?: string | null;
  cardName?: string | null;
  title?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  organization?: string | null;
}
