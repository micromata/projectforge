/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.api.impl.CustomResultFilter

private val log = KotlinLogging.logger {}

class AuftragFakturiertFilter(val values: List<AuftragFakturiertFilterStatus>) : CustomResultFilter<AuftragDO> {
    override fun match(list: MutableList<AuftragDO>, element: AuftragDO): Boolean {
        if (values.isEmpty() || values.contains(AuftragFakturiertFilterStatus.ALL)) {
            return true
        }
        log.error { "Not yet implemented." }
        return true
    }

    companion object {
        fun create(valuesAsStrings: Array<String>): AuftragFakturiertFilter {
            val values = valuesAsStrings.mapNotNull {
                try {
                    AuftragFakturiertFilterStatus.valueOf(it)
                } catch (ex: Exception) {
                    log.error { "Ignore unknown value '$it' of type ${AuftragFakturiertFilterStatus::class.java.name}." }
                    null
                }
            }
            return AuftragFakturiertFilter(values)
        }
    }
}
