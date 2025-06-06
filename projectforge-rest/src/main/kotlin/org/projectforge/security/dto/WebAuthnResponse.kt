/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.security.dto

/**
 * [Yubico developer guide](https://developers.yubico.com/WebAuthn/WebAuthn_Developer_Guide/WebAuthn_Client_Registration.html)
 *
 * Yubico: *contains the credential public key, and metadata which can be used by the RP to assess the characteristics
 * of the credential. The attestationObject contains the authenticator data and attestation statement. The
 * clientDataJSON contains the JSON-serialized data passed to the authenticator by the client in order to generate
 * the credential.*
 */
class WebAuthnResponse(
  var authenticatorData: String? = null, // For authentication only
  var clientDataJSON: String? = null,
  var attestationObject: String? = null, // For registration only.
  var signature: String? = null,         // For authentication only
  var transports: Set<String>? = null,   // For registration only.
  var userHandle: String? = null,        // For authentication only.
)
