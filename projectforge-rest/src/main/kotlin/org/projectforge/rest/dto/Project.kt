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
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.framework.i18n.translate

class Project(
    id: Long? = null,
    displayName: String? = null,
    var nummer: Int = 0,
    var name: String? = null,
    var identifier: String? = null,
    var description: String? = null,
    var status: ProjektStatus? = null,
    var customer: Customer? = null,
    var konto: Konto? = null,
    var task: Task? = null,
    var projektManagerGroup: Group? = null,
    var projectManager: User? = null,
    var headOfBusinessManager: User? = null,
    var salesManager: User? = null,
    var nummernkreis: Int? = null,
    var bereich: Int? = null,
    /**
     * The internal range (Kostenstellen 2-4, "4.xxx") of a project without customer; its customer's number
     * takes this place otherwise.
     */
    var internKost2_4: Int? = null,
    /**
     * All cost 2 types, for the edit form: the ones the project already has are marked
     * [Kost2Art.existsAlready], the ones picked for creation [Kost2Art.selected]. In a list row only the
     * project's existing ones, with [Kost2Art.active] (see `ProjectEntityRest.createListRow`).
     */
    var kost2Arts: List<Kost2Art>? = null,
    var kostFormatted: String? = null,
    /**
     * The two-digit ids of the project's cost 2 types, for the list column only.
     */
    var kost2ArtsAsString: String? = null,
    /**
     * True if the project has cost 2 units, so its number and customer can't be changed any more
     * ([org.projectforge.business.fibu.ProjektDao.isNumberLocked]). For the edit form only; ignored when
     * posted back.
     */
    var numberLocked: Boolean? = null,
) : BaseDTODisplayObject<ProjektDO>(id, displayName = displayName) {
    @get:JsonProperty
    val statusAsString: String?
        get() {
            status?.let { return translate(it.i18nKey) }
            return null
        }

    /**
     * @see copyFromMinimal
     */
    constructor(src: ProjektDO) : this() {
        copyFromMinimal(src)
    }

    override fun copyFromMinimal(src: ProjektDO) {
        super.copyFromMinimal(src)
        // Resolve the customer through the cache: src may be a detached/cached ProjektDO whose kunde is
        // a lazy proxy. KundeDO.id is @Transient and initializes the whole proxy on access (-> "statement
        // closed"), so build the Customer from the fully initialized cached KundeDO instead.
        this.customer = KundeCache.instance.getKundeIfNotInitialized(src.kunde)?.let {
            Customer(it)
        }
    }

    override fun copyFrom(src: ProjektDO) {
        super.copyFrom(src)
        this.nummernkreis = src.nummernkreis
        this.bereich = src.bereich
        src.kunde?.let {
            // Name and division for the list columns of the next project list; the kunde is resolved
            // from the cache (PfCaches.initialize), so this costs no query.
            this.customer = Customer(it).also { customer ->
                customer.name = it.name
                customer.division = it.division
            }
        }
        src.konto?.let {
            this.konto = Konto(it)
        }
        src.task?.let {
            this.task = Task(it)
        }
        this.kostFormatted = KostFormatter.instance.formatProjekt(src, KostFormatter.FormatType.FORMATTED_NUMBER)
    }

    override fun copyTo(dest: ProjektDO) {
        super.copyTo(dest)
        // The DTO field is named 'customer' while the DO field is 'kunde', so the name-based super.copyTo
        // skips it; map it here by id (null clears the reference, i.e. an internal project).
        dest.kunde = customer?.id?.let { id -> KundeDO().also { it.id = id } }
    }
}
