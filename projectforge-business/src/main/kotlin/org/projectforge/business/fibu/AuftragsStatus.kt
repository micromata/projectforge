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

import org.projectforge.common.i18n.I18nEnum

/**
 * ERSETZT: Angebot wurde durch ein überarbeitetes oder neues Angebot ersetzt. LOI: Es liegt eine Absichtserklärung vor (reicht nur bei
 * langjährigen Kunden, um mit den Arbeiten zu beginnen). GROB_KALKULATION: Es wird lediglich eine Schätzung oder eine Grobkalkulation dem
 * Kunden kommuniziert.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
enum class AuftragsStatus(
    /**
     * @return The key suffix will be used e. g. for i18n.
     */
    val key: String
) : I18nEnum {
    IN_ERSTELLUNG("in_erstellung"),
    POTENZIAL("potenzial"),
    GELEGT("gelegt"),
    LOI("loi"),
    BEAUFTRAGT("beauftragt"),
    ABGESCHLOSSEN("abgeschlossen"),
    ABGELEHNT("abgelehnt"),
    ERSETZT("ersetzt"),
    ESKALATION("eskalation");

    val orderState: AuftragsOrderState
        get() = when (this) {
            IN_ERSTELLUNG, POTENZIAL, GELEGT, LOI -> AuftragsOrderState.POTENTIAL
            BEAUFTRAGT, ABGESCHLOSSEN, ESKALATION -> AuftragsOrderState.ORDERED
            ABGELEHNT, ERSETZT -> AuftragsOrderState.LOST
        }

    override val i18nKey: String
        /**
         * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
         */
        get() = "fibu.auftrag.status.$key"

    /**
     * Converts this to equivalent enum AuftragsPositionsStatus.
     * @return
     */
    fun asAuftragsPositionStatus(): AuftragsPositionsStatus {
        return when (this) {
            IN_ERSTELLUNG -> AuftragsPositionsStatus.IN_ERSTELLUNG
            POTENZIAL -> AuftragsPositionsStatus.POTENZIAL
            GELEGT -> AuftragsPositionsStatus.GELEGT
            LOI -> AuftragsPositionsStatus.LOI
            BEAUFTRAGT -> AuftragsPositionsStatus.BEAUFTRAGT
            ABGESCHLOSSEN -> AuftragsPositionsStatus.ABGESCHLOSSEN
            ABGELEHNT -> AuftragsPositionsStatus.ABGELEHNT
            ERSETZT -> AuftragsPositionsStatus.ERSETZT
            ESKALATION -> AuftragsPositionsStatus.ESKALATION
        }
    }

    fun isIn(vararg status: AuftragsStatus): Boolean {
        return status.contains(this)
    }

    fun isNotIn(vararg status: AuftragsStatus): Boolean {
        return !isIn(status = status)
    }

    companion object {
        fun safeValueOf(name: String?): AuftragsStatus? {
            name ?: return null
            return AuftragsStatus.entries.firstOrNull { it.name == name }
        }
    }
}
