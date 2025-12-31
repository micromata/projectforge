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

package org.projectforge.business.fibu

import org.projectforge.common.i18n.I18nEnum

/**
 * @author Florian Blumenstein
 */
enum class AuftragsPositionsPaymentType(key: String) : I18nEnum {
    FESTPREISPAKET("festpreispaket"), TIME_AND_MATERIALS("time_and_materials"), PAUSCHALE("pauschale");

    private val key: String

    /**
     * The key will be used e. g. for i18n.
     *
     * @return
     */
    fun getKey(): String {
        return key
    }

    init {
        this.key = key
    }

    fun isIn(vararg auftragsPositionsPaymentType: AuftragsPositionsPaymentType?): Boolean {
        for (paymentType in auftragsPositionsPaymentType) {
            if (this == paymentType) {
                return true
            }
        }
        return false
    }

    override val i18nKey: String?
        get() = "fibu.auftrag.position.paymenttype." + key

    companion object {
        fun safeValueOf(name: String?): AuftragsPositionsPaymentType? {
            name ?: return null
            return AuftragsPositionsPaymentType.entries.firstOrNull { it.name == name }
        }
    }
}
