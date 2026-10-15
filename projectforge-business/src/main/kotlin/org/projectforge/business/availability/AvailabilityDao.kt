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

package org.projectforge.business.availability

import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.SortOrder
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * DAO for availability entries.
 */
@Service
open class AvailabilityDao : BaseDao<AvailabilityDO>(AvailabilityDO::class.java) {
    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun newInstance(): AvailabilityDO {
        return AvailabilityDO()
    }

    override fun hasUserSelectAccess(user: PFUserDO, throwException: Boolean): Boolean {
        return true
    }

    override fun hasUserSelectAccess(user: PFUserDO, obj: AvailabilityDO, throwException: Boolean): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        // The members of the user's groups, as for vacations (VacationDao): not the entries of others that ended
        // more than VISIBLE_PAST_DAYS ago.
        if (obj.user != null && accessChecker.areUsersInSameGroup(user, obj.user) &&
            obj.endDate?.isAfter(LocalDate.now().minusDays(VISIBLE_PAST_DAYS)) == true
        ) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasInsertAccess(user: PFUserDO, obj: AvailabilityDO?, throwException: Boolean): Boolean {
        requireNotNull(obj) { "Given AvailabilityDO as obj parameter mustn't be null." }
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasUpdateAccess(
        user: PFUserDO,
        obj: AvailabilityDO,
        dbObj: AvailabilityDO?,
        throwException: Boolean
    ): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasDeleteAccess(
        user: PFUserDO,
        obj: AvailabilityDO,
        dbObj: AvailabilityDO?,
        throwException: Boolean
    ): Boolean {
        if (hasHrRights(user) || isOwnEntry(user, obj)) {
            return true
        }
        return throwOrReturnFalse(throwException)
    }

    override fun hasHistoryAccess(user: PFUserDO, obj: AvailabilityDO, throwException: Boolean): Boolean {
        return if (hasHrRights(user) || isOwnEntry(user, obj)) {
            true
        } else throwOrReturnFalse(throwException)
    }

    override fun onInsert(obj: AvailabilityDO) {
        validate(obj, throwException = true)
    }

    override fun onUpdate(obj: AvailabilityDO, dbObj: AvailabilityDO) {
        validate(obj, throwException = true)
    }

    /**
     * An undeleted entry is validated like a new one: meanwhile its period may be used by another entry.
     */
    override fun onUndelete(obj: AvailabilityDO) {
        obj.deleted = false // The validation skips deleted entries.
        validate(obj, throwException = true)
    }

    /**
     * Checks the period of the given entry: start and end date must be set, the end date mustn't be before the start
     * date and no other entry of the same user may overlap it.
     * @return The i18n key of the error or null, if the entry is valid.
     */
    open fun validate(obj: AvailabilityDO, throwException: Boolean = false): String? {
        val startDate = obj.startDate
        val endDate = obj.endDate
        val error = when {
            startDate == null || endDate == null -> ERROR_DATE_NOT_SET
            endDate.isBefore(startDate) -> ERROR_END_DATE_BEFORE_START_DATE
            !obj.deleted && getOverlappingEntries(obj.user?.id, startDate, endDate).any { it.id != obj.id } ->
                ERROR_COLLISION

            else -> null
        }
        if (error != null && throwException) {
            throw UserException(error)
        }
        return error
    }

    /**
     * The (not deleted) entries of the given user overlapping the given period.
     */
    open fun getOverlappingEntries(userId: Long?, startDate: LocalDate, endDate: LocalDate): List<AvailabilityDO> {
        userId ?: return emptyList()
        return persistenceService.executeQuery(
            "SELECT a FROM AvailabilityDO a WHERE a.user.id = :userId AND a.endDate >= :startDate AND a.startDate <= :endDate AND a.deleted = false",
            AvailabilityDO::class.java,
            Pair("userId", userId),
            Pair("startDate", startDate),
            Pair("endDate", endDate),
        )
    }

    override fun afterLoad(obj: AvailabilityDO) {
        if (!obj.internalComment.isNullOrBlank()) {
            // No logged-in user means this runs during background cache maintenance (AvailabilityCache.refresh), where
            // no per-user privacy decision can be made. Keep the raw comment in the shared cache; consumers serving
            // cached entries (calendar, vacation export) apply isInternalCommentVisibleFor per logged-in user.
            val user = ThreadLocalUserContext.loggedInUser ?: return
            if (!isInternalCommentVisibleFor(user, obj)) {
                obj.internalComment = null
            }
        }
    }

    /**
     * The internal comment is personal data: it's only visible to the user and to HR staff. Callers that serve
     * entries from the shared [AvailabilityCache] must apply this check per logged-in user, because the cached comment
     * is intentionally kept raw (see [afterLoad]).
     */
    open fun isInternalCommentVisibleFor(user: PFUserDO, obj: AvailabilityDO): Boolean {
        return hasHrRights(user) || isOwnEntry(user, obj)
    }

    /**
     * What the entry is called for the given viewer: its type for the user and HR staff ([label]), only its status for
     * everybody else and without a viewer, e.g. in a calendar subscription ([neutralLabel]).
     */
    open fun labelFor(viewer: PFUserDO?, obj: AvailabilityDO): String {
        return if (viewer != null && isInternalCommentVisibleFor(viewer, obj)) obj.label else obj.neutralLabel
    }

    open fun hasHrRights(user: PFUserDO?): Boolean {
        return accessChecker.hasRight(user, UserRightId.HR_VACATION, false, UserRightValue.READWRITE)
    }

    private fun isOwnEntry(user: PFUserDO, obj: AvailabilityDO): Boolean {
        return obj.user?.id != null && obj.user?.id == user.id
    }

    private fun throwOrReturnFalse(throwException: Boolean): Boolean {
        if (throwException) {
            throw AccessException("access.exception.userHasNotRight", UserRightId.HR_VACATION, UserRightValue.READWRITE)
        }
        return false
    }

    companion object {
        const val ERROR_DATE_NOT_SET = "availability.validate.dateNotSet"
        const val ERROR_END_DATE_BEFORE_START_DATE = "availability.validate.endBeforeStart"
        const val ERROR_COLLISION = "availability.validate.collision"

        /** How long the entries of others stay visible to the members of their groups after their end date. */
        const val VISIBLE_PAST_DAYS = 31L

        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("user.firstname", "user.lastname", "user.username")
        private val DEFAULT_SORT_PROPERTIES = arrayOf(
            SortProperty("user.firstname"),
            SortProperty("user.lastname"),
            SortProperty("startDate", SortOrder.DESCENDING)
        )
    }
}
