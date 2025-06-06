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

package org.projectforge.business.fibu

import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.KostZuweisungDO
import java.io.Serializable

/**
 * Embedded by [RechnungPosInfo].
 */
class KostZuweisungInfo(kostZuweisung: KostZuweisungDO) : Serializable {
    data class Kost(val formattedNumber: String, val displayName: String?): Serializable

    val id = kostZuweisung.id
    val netto = kostZuweisung.netto
    val kost1: Kost
    val kost2: Kost

    init {
        val kostCache = KostCache.instance
        val kost1DO = kostCache.getKost1(kostZuweisung.kost1Id)
        val kost2DO = kostCache.getKost2(kostZuweisung.kost2Id)
        val kost1Text = KostFormatter.instance.formatKost1(kost1DO, KostFormatter.FormatType.TEXT)
        val kost2Text = KostFormatter.instance.formatKost2(kost2DO, KostFormatter.FormatType.TEXT)
        kost1 = Kost(kost1DO?.formattedNumber ?: "???", kost1Text)
        kost2 = Kost(kost2DO?.formattedNumber ?: "???",kost2Text)
    }
}
