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

// The address book of the app (AddressEntityRest, GET /rs/address/appList; the images of AddressImageServicesRest, the vCard of
// AddressServicesRest).
// See src/rs/app-auth.ts for @server.

/**
 * All addresses the user may see, or with `modifiedSince` the changed ones and the ids of all.
 * @server org.projectforge.rest.AddressEntityRest$AppAddressList
 */
export interface AppAddressList {
  /** The `modifiedSince` of the next request. */
  timestamp: number;
  /** The ids of all addresses the user may see (the ones missing are gone). */
  ids: number[];
  favoriteIds?: number[];
  addresses: AppAddress[];
  /** The addresses come with their vCards (missing from servers before 9.2.6). */
  withVCards?: boolean;
}

/** @server org.projectforge.rest.AddressEntityRest$AppAddress */
export interface AppAddress {
  id: number;
  title?: string | null;
  firstName?: string | null;
  name?: string | null;
  organization?: string | null;
  division?: string | null;
  positionText?: string | null;
  businessPhone?: string | null;
  mobilePhone?: string | null;
  fax?: string | null;
  privatePhone?: string | null;
  privateMobilePhone?: string | null;
  email?: string | null;
  privateEmail?: string | null;
  website?: string | null;
  business?: AppPostalAddress | null;
  postal?: AppPostalAddress | null;
  private?: AppPostalAddress | null;
  /** ISO date `yyyy-MM-dd`. */
  birthday?: string | null;
  comment?: string | null;
  hasImage?: boolean;
  writeAccess?: boolean;
  /** The version of the address and its image: an image on the device is valid for the same value. */
  lastUpdate: number;
  /** The vCard of the address without the photo, for sharing (missing from servers before 9.2.6). */
  vCard?: string | null;
}

/** @server org.projectforge.rest.AddressEntityRest$AppPostalAddress */
export interface AppPostalAddress {
  addressText?: string | null;
  addressText2?: string | null;
  zipCode?: string | null;
  city?: string | null;
  state?: string | null;
  country?: string | null;
}
