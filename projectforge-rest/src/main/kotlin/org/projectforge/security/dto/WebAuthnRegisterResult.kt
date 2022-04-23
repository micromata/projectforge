/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredentialParameters

class WebAuthnRegisterResult(
  var rp: WebAuthnRp,
  var user: WebAuthnUser,
  var challenge: String,
  var pubKeyCredParams: Array<PublicKeyCredentialParameters>,
  var authenticatorSelection: AuthenticatorSelectionCriteria,
  var attestation: String = "direct",
  var extensions: WebAuthnExtensions,
)

/*
{
  "publicKey": {
    "rp": {
      "name": "Yubico WebAuthn demo",
      "id": "localhost"
    },
    "user": {
      "name": "kai",
      "displayName": "kai",
      "id": "jTNp7UFg4AmWu1-SdW2LJtwOglXnoVS4CItD1HHkGQU"
    },
    "challenge": "8InUMLpDVCMJZI0PrSbbQXjDWACGJm96XoHkr8_Ixis",
    "pubKeyCredParams": [
      {
        "alg": -7,
        "type": "public-key"
      },
      {
        "alg": -8,
        "type": "public-key"
      },
      {
        "alg": -257,
        "type": "public-key"
      }
    ],
    "excludeCredentials": [],
    "authenticatorSelection": {
      "requireResidentKey": false,
      "residentKey": "discouraged",
      "userVerification": "preferred"
    },
    "attestation": "direct",
    "extensions": {
      "appidExclude": "https://localhost:8443",
      "credProps": true
    }
  }
} */
