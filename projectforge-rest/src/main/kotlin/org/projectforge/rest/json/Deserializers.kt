/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.IntNode
import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.math.BigDecimal


/**
 * Deserialization for PFUserDO.
 */
class PFUserDODeserializer : StdDeserializer<PFUserDO>(PFUserDO::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): PFUserDO? {
        val node: JsonNode = p.getCodec().readTree(p)
        val id = (node.get("id") as IntNode).numberValue() as Int
        val user = PFUserDO()
        user.id = id
        return user
    }
}


/**
 * Deserialization for Integers.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class IntDeserializer : StdDeserializer<Integer>(Integer::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Integer? {
        val str = p.getText()
        if (StringUtils.isBlank(str)) {
            return null
        }
        try {
            return Integer(str)
        } catch (ex: NumberFormatException) {
            throw ctxt.weirdStringException(str, Integer::class.java, "Can't parse integer.")
        }
    }
}

/**
 * Deserialization for BigDecimals.
 */
class BigDecimalDeserializer : StdDeserializer<BigDecimal>(BigDecimal::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BigDecimal? {
        val str = p.getText()
        if (StringUtils.isBlank(str)) {
            return null
        }
        try {
            return BigDecimal(str)
        } catch (ex: NumberFormatException) {
            throw ctxt.weirdStringException(str, BigDecimal::class.java, "Can't parse decimal number.")
        }
    }
}
