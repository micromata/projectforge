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

import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.KostFormatter.Companion.ABBREVIATION_LENGTH
import org.projectforge.business.fibu.KostFormatter.FormatType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class KundeFormatter {
    @Autowired
    private lateinit var kostFormatter: KostFormatter

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    /**
     * @see KostFormatter.formatKunde
     */
    @JvmOverloads
    fun format(
        kundeId: Long?,
        formatType: FormatType = FormatType.FORMATTED_NUMBER,
        abbreviationLength: Int = ABBREVIATION_LENGTH,
    ): String {
        return kostFormatter.formatKunde(kundeId, formatType, abbreviationLength)
    }

    /**
     * @see KostFormatter.formatKunde
     */
    @JvmOverloads
    fun format(
        kunde: KundeDO?,
        formatType: FormatType = FormatType.FORMATTED_NUMBER,
        abbreviationLength: Int = ABBREVIATION_LENGTH,
    ): String {
        return format(kunde?.id, formatType, abbreviationLength)
    }

    companion object {
        @JvmStatic
        lateinit var instance: KundeFormatter
            private set

        /**
         * Displays customers and/or kundeText as String.
         *
         * @param kunde null supported.
         * @param kundeText null supported.
         * @return
         */
        @JvmStatic
        fun formatKundeAsString(kunde: KundeDO?, kundeText: String?): String {
            return listOfNotNull(
                kundeText.takeIf { !it.isNullOrBlank() },
                kunde?.name.takeIf { !kunde?.name.isNullOrBlank() }
            ).joinToString(separator = "; ")
        }
    }
}
