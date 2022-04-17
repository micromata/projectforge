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

package org.projectforge.security

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode

/**
 * My2FA may have more fields (if used in derived classes). But, if used in MyFAServicesRest, all other
 * fields have to be ignored.
 */
open class My2FADataDeserializer : StdDeserializer<My2FAData>(My2FAData::class.java) {
  override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): My2FAData? {
    val node: JsonNode = p.codec.readTree(p)
    val code = (node.get("code") as TextNode?)?.textValue()
    val target = (node.get("target") as TextNode?)?.textValue()
    val password = (node.get("password") as TextNode?)?.textValue()
    val lastSuccessful2FA = (node.get("lastSuccessful2FA") as TextNode?)?.textValue()
    val data = My2FAData()
    data.code = code
    data.password = password?.toCharArray()
    data.lastSuccessful2FA = lastSuccessful2FA
    data.target = target
    return data
  }
}
