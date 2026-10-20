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

import org.projectforge.business.fibu.customergroup.OrderAccessGrantService
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate

/**
 * What the read access to an order of a user without full order-book access asks, taken from an order entity or
 * from the [AuftragsCache]'s [OrderInfo] (no lazy loading).
 *
 * @param isContact Whether a user is the order's contact person or one of its additional contacts.
 * @param kundeId The number of the order's customer entity.
 * @param vollstaendigFakturiert Computed only if the user is tied to the order or granted it.
 * @param projekt The order's project, if not in the [ProjektCache] (yet).
 */
class OrderAccessFacts(
    val isContact: (Long?) -> Boolean,
    val kundeId: Long?,
    val kundeText: String?,
    val projektId: Long?,
    val vollstaendigFakturiert: () -> Boolean,
    val periodOfPerformanceEnd: LocalDate?,
    val angebotsDatum: LocalDate?,
    val projekt: ProjektDO? = null,
) {
    companion object {
        fun of(order: AuftragDO) = OrderAccessFacts(
            isContact = { order.isContact(it) },
            // nummer, not the id alias, reads the foreign key of a lazy proxy without loading it (see KundeDO).
            kundeId = order.kunde?.nummer,
            kundeText = order.kundeText,
            projektId = order.projekt?.id,
            vollstaendigFakturiert = { AuftragsCache.instance.isVollstaendigFakturiert(order) },
            periodOfPerformanceEnd = order.periodOfPerformanceEnd,
            angebotsDatum = order.angebotsDatum,
            projekt = order.projekt,
        )

        fun of(info: OrderInfo) = OrderAccessFacts(
            isContact = { info.isContact(it) },
            kundeId = info.kundeId,
            kundeText = info.kundeText,
            projektId = info.projektId,
            vollstaendigFakturiert = { info.isVollstaendigFakturiert },
            periodOfPerformanceEnd = info.periodOfPerformanceEnd,
            angebotsDatum = info.angebotsDatum,
        )
    }
}

/**
 * The read access to an order of a user without full order-book access (project managers and assistants, users
 * with [org.projectforge.business.user.UserRightValue.PARTLYREADWRITE]), shared by [AuftragRight] and the
 * cache-only checks of [RechnungDao]: the user is tied to the order (its contact person or an additional
 * contact, a contact of its project or a member of the project's manager group) or, for reading only, granted it
 * by an order access grant ([OrderAccessGrantService]). A fully invoiced order is visible only for
 * [AuftragRight.MAX_DAYS_OF_VISIBILITY_4_PROJECT_MANGER] days after its period of performance (else its offer
 * date).
 *
 * The general right and the shortcuts of controlling and the finance/orga groups are checked by the callers.
 */
object OrderSelectAccess {
    /**
     * @param includeGrants True for reading only: a grant never allows writing.
     */
    fun hasAccess(user: PFUserDO, facts: OrderAccessFacts, includeGrants: Boolean): Boolean {
        if (!isTied(user, facts) && !(includeGrants && isGranted(user, facts))) {
            return false
        }
        if (!facts.vollstaendigFakturiert()) {
            return true
        }
        val endDate = facts.periodOfPerformanceEnd ?: facts.angebotsDatum ?: return false
        return LocalDate.now().toEpochDay() - endDate.toEpochDay() <= AuftragRight.MAX_DAYS_OF_VISIBILITY_4_PROJECT_MANGER
    }

    /** The contact person, an additional contact, a contact of the project or a member of its manager group. */
    fun isTied(user: PFUserDO, facts: OrderAccessFacts): Boolean {
        if (facts.isContact(user.id)) {
            return true
        }
        // The project from the cache: the next/REST layer checks the posted order, whose projekt is a stub carrying
        // only its id (Auftrag.copyTo), without manager group and contacts.
        val projekt = ProjektCache.instance.getProjekt(facts.projektId) ?: facts.projekt ?: return false
        return UserGroupCache.getInstance().isUserMemberOfGroup(user.id, projekt.projektManagerGroupId)
                || projekt.isContact(user.id)
    }

    fun isGranted(user: PFUserDO, facts: OrderAccessFacts): Boolean =
        OrderAccessGrantService.instanceOrNull?.isGranted(user.id, facts.kundeId, facts.kundeText, facts.projektId) == true
}
