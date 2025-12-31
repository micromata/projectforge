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

import org.apache.commons.collections4.CollectionUtils
import org.projectforge.business.user.*
import org.projectforge.business.user.UserGroupCache.Companion.getInstance
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.web.WicketSupport
import java.time.LocalDate

/**
 * User [UserRightValue.PARTLYREADWRITE] for users who are members of FIBU_ORGA_GROUPS **and** of
 * PROJECT_MANAGER/PROJECT_ASSISTANT: If set, then such users have only access to their projects (assigned by the
 * project manager groups). If you choose [UserRightValue.READWRITE] for such users, they'll have full read/write
 * access to all orders.
 *
 * @author Kai Reinhard (k.reinhard@me.de)
 */
class AuftragRight() : UserRightAccessCheck<AuftragDO?>(
    UserRightId.PM_ORDER_BOOK, UserRightCategory.PM,
    *UserRightServiceImpl.FALSE_READONLY_PARTLYREADWRITE_READWRITE
) {
    /**
     * @return True, if [UserRightId.PM_PROJECT] is potentially available for the user (independent from the
     * configured value).
     * @see org.projectforge.business.user.UserRightAccessCheck.hasSelectAccess
     */
    override fun hasSelectAccess(user: PFUserDO): Boolean {
        return WicketSupport.getAccessChecker().hasRight(
            user, id, UserRightValue.READONLY,
            UserRightValue.PARTLYREADWRITE,
            UserRightValue.READWRITE
        )
    }

    /**
     * Contact persons sehen Aufträge, die ihnen zugeordnet sind und die nicht vollständig fakturiert sind, sonst wie
     * hasSelectAccess(boolean). Vollständig fakturierte Aufträge sehen die contact persons nur, wenn das Angebotsdatum
     * nicht älter ca. 5 Jahre (ca. 1800 Tage) ist. <br></br>
     * Ebenso sehen Projektmanager und Projektassistenten einen Auftrag analog zu einer Kontaktperson, sofern sie Mitglied
     * der ProjektManagerGroup des zugordneten Projekts sind. <br></br>
     * Nur Mitglieder der FINANCE_GROUP dürfen für Aufträge das Flag "vollständig fakturiert" ändern.
     *
     * @see org.projectforge.framework.persistence.api.BaseDao.hasAccess
     */
    override fun hasAccess(
        user: PFUserDO, obj: AuftragDO?, oldObj: AuftragDO?,
        operationType: OperationType
    ): Boolean {
        val userGroupCache = getInstance()
        val accessChecker = WicketSupport.getAccessChecker()
        if (operationType == OperationType.SELECT) {
            if (accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.CONTROLLING_GROUP)) {
                return true
            }
            if (!accessChecker.hasRight(
                    user, id, UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE,
                    UserRightValue.READWRITE
                )
            ) {
                return false
            }
        } else {
            if (!accessChecker.hasRight(user, id, UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE)) {
                return false
            }
        }
        if (obj != null && !accessChecker.isUserMemberOfGroup(user, ProjectForgeGroup.FINANCE_GROUP)
            && CollectionUtils.isNotEmpty(obj.positionenIncludingDeleted)
        ) {
            // Special field check for non finance administrative staff members:
            if (operationType == OperationType.INSERT) {
                for (position in obj.positionenExcludingDeleted) {
                    if (position.vollstaendigFakturiert!!) {
                        throw AccessException("fibu.auftrag.error.vollstaendigFakturiertProtection")
                    }
                }
            } else if (oldObj != null) {
                for (number in 1..obj.positionenIncludingDeleted!!.size) {
                    val position = obj.getPosition(number.toShort()) ?: continue
                    val dbPosition = oldObj.getPosition(number.toShort())

                    // check if deleted
                    if (position.deleted == true) continue
                    if (dbPosition == null) {
                        if (position.vollstaendigFakturiert == true) {
                            throw AccessException("fibu.auftrag.error.vollstaendigFakturiertProtection")
                        }
                    } else if (position.vollstaendigFakturiert != dbPosition.vollstaendigFakturiert) {
                        throw AccessException("fibu.auftrag.error.vollstaendigFakturiertProtection")
                    }
                }
            }
        }
        if (accessChecker.isUserMemberOfGroup(user, *UserRightServiceImpl.FIBU_ORGA_GROUPS)
            && accessChecker.hasRight(user, id, UserRightValue.READONLY, UserRightValue.READWRITE)
        ) {
            // No further access checking (but not for users with right PARTLY_READWRITE.
        } else if (obj != null) {
            // User should be a PROJECT_MANAGER or PROJECT_ASSISTANT or user has PARTLYREADWRITE access:
            var hasAccess = false
            if (accessChecker.userEquals(user, obj.contactPerson)) {
                hasAccess = true
            }
            obj.projekt?.let { projekt ->
                if (userGroupCache.isUserMemberOfGroup(user.id, projekt.projektManagerGroupId)
                    || projekt.headOfBusinessManagerId == user.id
                    || projekt.salesManagerId == user.id
                ) {
                    hasAccess = true
                }
            }
            if (hasAccess) {
                if (!AuftragsCache.instance.isVollstaendigFakturiert(obj)) {
                    return true
                }
                (obj.periodOfPerformanceEnd ?: obj.angebotsDatum)?.let { endDate ->
                    val days = LocalDate.now().toEpochDay() - endDate.toEpochDay()
                    return days <= MAX_DAYS_OF_VISIBILITY_4_PROJECT_MANGER
                }
            }
            return false
        }
        return true
    }

    companion object {
        /**
         * Orders older than this number of days will not be visible for project managers anymore.
         */
        const val MAX_DAYS_OF_VISIBILITY_4_PROJECT_MANGER = 1800
        private const val serialVersionUID = 8639987084144268831L
    }

    init {
        initializeUserGroupsRight(
            UserRightServiceImpl.FALSE_READONLY_PARTLYREADWRITE_READWRITE,
            *UserRightServiceImpl.FIBU_ORGA_PM_GROUPS
        ) // All project managers have read-write access:
            .setAvailableGroupRightValues(
                ProjectForgeGroup.PROJECT_MANAGER,
                UserRightValue.PARTLYREADWRITE
            ) // All project assistants have no, read or read-write access:
            .setAvailableGroupRightValues(
                ProjectForgeGroup.PROJECT_ASSISTANT, UserRightValue.FALSE,
                UserRightValue.PARTLYREADWRITE
            ) // Read only access for controlling users:
            .setReadOnlyForControlling()
    }
}
