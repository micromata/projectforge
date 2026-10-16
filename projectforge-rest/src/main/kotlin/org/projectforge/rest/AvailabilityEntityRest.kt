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


package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.availability.AvailabilityConfig
import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.AvailabilityLocation
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Availability
import org.projectforge.rest.dto.User
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterObjectElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the availability entries (menu "Availability"), serving the hand-built list and
 * edit page of projectforge-next (see components/features/availability). Replaces the React UILayout
 * `AvailabilityPagesRest`:
 *
 * - an entry is of a user, not of an employee: freelancers without an employee record enter their availability, too,
 * - a new entry is of the logged-in user (HR staff may preset another one by `?user=<id>`),
 * - the user is changeable by HR staff only ([Availability.userEditable]),
 * - the types offered are the configured ones ([AvailabilityConfig], [Availability.availableTypes]),
 * - the internal comment is offered to the user and HR staff only ([Availability.internalCommentVisible]); for
 *   anybody else the dao doesn't send it nor the type ([AvailabilityDao.afterLoad]).
 */
@RestController
@RequestMapping("${Rest.URL}/availability")
class AvailabilityEntityRest :
    AbstractDTOEntityRest<AvailabilityDO, Availability, AvailabilityDao>(
        AvailabilityDao::class.java,
        "availability.title"
    ) {

    @Autowired
    private lateinit var availabilityConfig: AvailabilityConfig

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    /**
     * Status and location are shown translated: sorted as displayed, not by the enum's name. The type as shown, too:
     * the order of the database would reveal the types hidden from the viewer ([AvailabilityDao.afterLoad]).
     */
    override val computedSortProperties: Map<String, (AvailabilityDO) -> Comparable<*>?>
        get() = mapOf(
            "type" to { obj -> obj.type },
            "status" to { obj -> obj.status?.let { translate(it.i18nKey) } },
            "location" to { obj -> obj.location?.let { translate(it.i18nKey) } },
        )

    override fun transformForDB(dto: Availability): AvailabilityDO {
        val availabilityDO = AvailabilityDO()
        dto.copyTo(availabilityDO)
        return availabilityDO
    }

    override fun transformFromDB(obj: AvailabilityDO, editMode: Boolean): Availability {
        val availability = Availability()
        caches.initialize(obj)
        availability.copyFrom(obj)
        if (editMode) {
            applyFormInfo(availability, obj)
        }
        return availability
    }

    override fun newBaseDTO(request: HttpServletRequest?): Availability {
        val result = Availability()
        val userDO = NumberHelper.parseLong(request?.getParameter("user"))
            ?.takeIf { hasHrRights() } // The HR vacation right is what entitles to choose any user.
            ?.let { userGroupCache.getUser(it) }
            ?: ThreadLocalUserContext.loggedInUser
        userDO?.let {
            val user = User()
            user.copyFromMinimal(it)
            result.user = user
        }
        result.status = AvailabilityStatus.NOT_AVAILABLE
        result.location = AvailabilityLocation.REMOTE
        val obj = AvailabilityDO()
        result.copyTo(obj)
        applyFormInfo(result, obj)
        return result
    }

    /** What the form needs beside the entry's values. */
    private fun applyFormInfo(availability: Availability, obj: AvailabilityDO) {
        val user = ThreadLocalUserContext.requiredLoggedInUser
        availability.userEditable = hasHrRights()
        availability.internalCommentVisible = obj.user == null || baseDao.isInternalCommentVisibleFor(user, obj)
        // The stored type is always offered, also one removed from the configuration meanwhile.
        availability.availableTypes = (availabilityConfig.getTypes() + listOfNotNull(obj.type?.takeIf { it.isNotBlank() }))
            .distinct()
    }

    /**
     * The user is mandatory (`required` of `@PropertyInfo` doesn't reach a reference, see
     * [org.projectforge.ui.ElementsRegistry]); the errors of [AvailabilityDao.validate] are shown at their fields, the
     * dao checks them on the write, too. Not for the entry of somebody else unless HR staff: the write is refused then
     * anyway, and the collision check mustn't tell anybody when that person is away.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Availability) {
        super.validate(validationErrors, dto)
        if (dto.user?.id == null) {
            validationErrors.add(
                ValidationError(
                    translateMsg("validation.error.fieldRequired", translate("availability.user")),
                    fieldId = "user",
                )
            )
            return
        }
        if (!hasHrRights() && dto.user?.id != ThreadLocalUserContext.loggedInUserId) {
            return // Refused by the access check of the write.
        }
        val availability = AvailabilityDO()
        dto.copyTo(availability)
        baseDao.validate(availability)?.let { error ->
            val fieldId = when {
                error == AvailabilityDao.ERROR_STATUS_NOT_SET -> "status"
                error == AvailabilityDao.ERROR_DATE_NOT_SET && dto.startDate == null -> "startDate"
                else -> "endDate"
            }
            validationErrors.add(ValidationError(translate(error), fieldId = fieldId))
        }
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("status", label = translate("availability.status"), defaultFilter = true)
                .buildValues(AvailabilityStatus::class.java)
        )
        elements.add(
            UIFilterListElement("location", label = translate("availability.location"), defaultFilter = true)
                .buildValues(AvailabilityLocation::class.java)
        )
        elements.add(
            UIFilterElement(
                "period",
                label = translate("timePeriod"),
                filterType = UIFilterElement.FilterType.DATE,
                defaultFilter = true,
            )
        )
        elements.add(
            UIFilterObjectElement(
                "user",
                label = translate("availability.user"),
                autoCompletion = AutoCompletion.getAutoCompletion4Users(),
            ).also {
                it.defaultFilter = true
            }
        )
    }

    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter
    ): List<CustomResultFilter<AvailabilityDO>>? {
        if (!hasHrRights()) {
            // The type and the internal comment are hidden from others (AvailabilityDao.afterLoad), but a filter or an
            // order by them in the query would still tell their values by the rows returned.
            source.entries.filter { it.field in PERSONAL_FIELDS }.forEach { it.synthetic = true }
            source.sortProperties.removeIf { it.property in PERSONAL_FIELDS }
        }
        // The entries overlapping the period, by their dates in the query: ending on or after its begin and beginning
        // on or before its end. Either bound may be open.
        source.entries.find { it.field == "period" }?.let { entry ->
            entry.synthetic = true
            PFDayUtils.parseDate(entry.value.fromValue)?.let { target.add(QueryFilter.ge("endDate", it)) }
            PFDayUtils.parseDate(entry.value.toValue)?.let { target.add(QueryFilter.le("startDate", it)) }
        }
        // The pickers send the user as `value.id`, which the generic processor doesn't read.
        source.entries.find { it.field == "user" }?.let { entry ->
            entry.synthetic = true
            (entry.value.id ?: entry.value.value?.toLongOrNull())?.let {
                target.add(QueryFilter.eq("user.id", it))
            }
        }
        return null
    }

    /** The list starts with the entries overlapping the current calendar year (the pinned period). */
    override fun newMagicFilter(): MagicFilter {
        val filter = super.newMagicFilter()
        val today = PFDay.now()
        filter.entries.add(MagicFilterEntry("period").also {
            it.value.fromValue = today.beginOfYear.isoString
            it.value.toValue = today.endOfYear.isoString
            it.value.periodKind = "year"
        })
        return filter
    }

    private fun hasHrRights(): Boolean = baseDao.hasHrRights(ThreadLocalUserContext.loggedInUser)

    companion object {
        /** Only for the user and HR staff (see [AvailabilityDao.isInternalCommentVisibleFor]). */
        private val PERSONAL_FIELDS = setOf("type", "internalComment")
    }
}
