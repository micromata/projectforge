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

package org.projectforge.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeStatus
import org.projectforge.framework.i18n.translate

class Customer(id: Long? = null,
               displayName: String? = null,
               var nummer: Long? = null,
               var name: String? = null,
               var identifier: String? = null,
               var division: String? = null,
               var status: KundeStatus? = null,
               var description: String? = null,
               var konto: Konto? = null,
               var kost: String? = null,
) : BaseDTODisplayObject<KundeDO>(id, displayName = displayName) {

    @get:JsonProperty
    val statusAsString: String?
        get() {
            status?.let { return translate(it.i18nKey) }
            return null
        }

    /**
     * @see copyFromMinimal
     */
    constructor(src: KundeDO) : this() {
        copyFromMinimal(src)
    }

    override fun copyFrom(src: KundeDO) {
        // Resolve the account from KontoCache and set it back on src *before* super.copyFrom: that copy is
        // reflective and reads KundeDO.konto to fill the DTO's account, which would initialize the lazy
        // proxy — one T_FIBU_KONTO SELECT per row when a list runs the full copy (i.e. a client that is
        // not detected as projectforge-next, so copyFrom4ListRow does not apply). Same idiom as
        // PfCaches.getKundeIfNotInitialized, which also assigns the cached account onto the kunde.
        src.konto = PfCaches.instance.getKontoIfNotInitialized(src.konto)
        super.copyFrom(src)
        this.id = src.nummer
        this.kost = src.kost
        this.konto = kontoOf(src)
    }

    /**
     * The lean row of the hand built next customer list (`customer.page.ts`): its columns are kost,
     * identifier, name, division, konto (by display name), status, description and the two audit dates.
     *
     * Overriding this is what keeps the list off the reflective full [copyFrom]: that one reads
     * `KundeDO.konto` per row to fill the account, and the association is `FetchType.LAZY`, so a list
     * of N customers costs N extra `T_FIBU_KONTO` SELECTs (the classic N+1). Filling the row by hand
     * and resolving the account from [KontoCache] via [kontoOf] answers every column without a query.
     * Same pattern as [Rechnung.copyFrom4ListRow].
     */
    override fun copyFrom4ListRow(src: KundeDO) {
        id = src.nummer
        deleted = src.deleted
        displayName = src.displayName
        // The two columns every next list offers, hidden until the user switches them on.
        copyAuditFieldsFrom(src)
        nummer = src.nummer
        name = src.name
        identifier = src.identifier
        division = src.division
        status = src.status
        description = src.description
        kost = src.kost
        konto = kontoOf(src)
    }

    /**
     * Resolves the account from [KontoCache] instead of initializing the lazy proxy, so it costs no
     * query when the cache is warm — see [copyFrom4ListRow]. The account is shown by its display name
     * only, which is the whole column.
     */
    private fun kontoOf(src: KundeDO): Konto? {
        return PfCaches.instance.getKontoIfNotInitialized(src.konto)?.let { account ->
            Konto().also { it.copyFromMinimal(account) }
        }
    }
}
