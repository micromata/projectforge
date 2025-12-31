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

package org.projectforge.jcr

import mu.KotlinLogging
import javax.jcr.Node
import javax.jcr.Property

private val log = KotlinLogging.logger {}

/**
 * For information.
 */
class PropertyInfo() {
    internal constructor(property: Property): this() {
        name = property.name
        if (property.isMultiple) {
            property.values?.let {
                values = it.map { ValueInfo(it) }.toTypedArray()
            }
        } else {
            property.value?.let {
                value = ValueInfo(it)
            }
        }
    }

    fun addToNode(node: Node) {
        value?.let {
            when (it.type) {
                PropertyTypeEnum.BOOLEAN -> node.setProperty(name, it.boolean == true)
                PropertyTypeEnum.STRING -> node.setProperty(name, it.string)
                PropertyTypeEnum.DATE ->  node.setProperty(name, it.date)
                PropertyTypeEnum.DECIMAL ->  node.setProperty(name, it.decimal)
                PropertyTypeEnum.DOUBLE ->  node.setProperty(name, it.double ?: 0.0)
                PropertyTypeEnum.LONG ->  node.setProperty(name, it.long ?: 0L)
                PropertyTypeEnum.BINARY -> {} // Attachments will not handled here.
                else -> {} // Not known and used by ProjectForge (might be internal property).
            }
        }
        values?.let {
            log.error { "Restoring values as array not supported. Skipping for node '$node.path'." }
        }
    }

    var name: String? = null
    var value: ValueInfo? = null
    var values: Array<ValueInfo>? = null
}
