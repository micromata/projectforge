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

import jakarta.annotation.PostConstruct
import org.hibernate.Hibernate
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.utils.BaseFormatter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.StringBuilder

@Service
class ProjektFormatter : BaseFormatter() {
    @Autowired
    private lateinit var kostFormatter: KostFormatter

    @Autowired
    private lateinit var kundeCache: KundeCache

    @Autowired
    private lateinit var projektCache: ProjektCache

    @Autowired
    private lateinit var projektDao: ProjektDao

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    /**
     * Formats given project as string.
     *
     * @param projekt The project to show.
     * @param showOnlyNumber If true then only the kost2 number will be shown.
     * @return
     */
    fun format(projekt: ProjektDO?, showOnlyNumber: Boolean): String? {
        var useProjekt = projekt
        if (projekt?.id != null && !Hibernate.isInitialized(projekt)) {
            useProjekt = projektCache.getProjekt(projekt.id)
        }
        if (useProjekt == null) {
            return ""
        }
        // final KundeDO kunde = projekt.getKunde();
        val hasAccess = projektDao.hasLoggedInUserSelectAccess(false)
        if (!hasAccess) {
            return null
        }
        return if (showOnlyNumber) {
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.FORMATTED_NUMBER)
        } else {
            kostFormatter.formatProjekt(projekt, KostFormatter.FormatType.TEXT)
        }
    }

    companion object {
        lateinit var instance: ProjektFormatter
            private set

        /**
         * Formats kunde, kundeText, projekt.kunde and projekt as string.
         *
         * @param projekt null supported.
         * @param kunde null supported.
         * @param kundeText null supported.
         * @return
         */
        @JvmStatic
        @JvmOverloads
        fun formatProjektKundeAsString(projekt: ProjektDO?, kunde: KundeDO? = null, kundeText: String? = null): String {
            val useProjekt = instance.projektCache.getProjektIfNotInitialized(projekt)
            val projektKunde  = instance.kundeCache.getKundeIfNotInitialized(useProjekt?.kunde)
            val useKunde = instance.kundeCache.getKundeIfNotInitialized(kunde)
            return formatProjektKundeAsStringWithoutCache(useProjekt, projektKunde, useKunde, kundeText)
        }

        @JvmOverloads
        @JvmStatic
        fun formatProjektKundeAsStringWithoutCache(
            projekt: ProjektDO?,
            projektKunde: KundeDO?,
            kunde: KundeDO? = null,
            kundeText: String? = null
        ): String {
            val parts = mutableListOf<String>()
            kundeText?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            kunde?.name?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
            projektKunde?.name?.takeIf { it.isNotBlank() && !parts.contains(it) }?.let { parts.add(it) }
            val sb = StringBuilder()
            sb.append(parts.joinToString(separator = "; "))
            projekt?.name?.takeIf { it.isNotBlank() }?.let { sb.append(" - ").append(it) }
            return sb.toString()
        }
    }
}
