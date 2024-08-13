/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.business.fibu

import mu.KotlinLogging
import org.hibernate.search.engine.backend.document.DocumentElement
import org.hibernate.search.mapper.pojo.bridge.TypeBridge
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext

private val log = KotlinLogging.logger {}

/**
 * Bridge for hibernate search to search for order positions of form ###.## (&lt;order number&gt;.&lt;position
 * number&gt>).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class HibernateSearchAuftragsPositionBridge : TypeBridge<AuftragsPositionDO> {
    override fun write(
        target: DocumentElement,
        bridgedElement: AuftragsPositionDO,
        context: TypeBridgeWriteContext
    ) {
        val auftrag = bridgedElement.auftrag
        val sb = StringBuilder()
        if (auftrag?.nummer == null) {
            log.error("AuftragDO for AuftragsPositionDO: " + bridgedElement.id + "  is null.")
            target.addValue("position", "")
            return
        }
        sb.append(auftrag.nummer).append(".").append(bridgedElement.number.toInt())
        if (log.isDebugEnabled) {
            log.debug(sb.toString())
        }
        target.addValue("position", sb.toString())
    }
}
