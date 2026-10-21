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

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.timesheet.TimesheetVacationBookingConfig
import org.projectforge.framework.configuration.Configuration as ConfigurationValues
import org.projectforge.framework.configuration.ConfigurationCategory
import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationJsonValidators
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.ConfigurationType
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Configuration
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The system-configuration list and edit page (`configuration.page.tsx` in projectforge-next),
 * migrated from the Wicket `ConfigurationListPage`/`ConfigurationEditPage`.
 *
 * A flat list of the fixed set of configuration parameters (the
 * [org.projectforge.framework.configuration.ConfigurationParam] enum), each opening its own edit page
 * whose value input is rendered per [org.projectforge.framework.configuration.ConfigurationType]. The
 * set is fixed: there is no add and no delete (suppressed in [ConfigurationDao]).
 *
 * Layout free ([AbstractDTOEntityRest]): the hand built next page brings its own layout, so there is no
 * server side `createListLayout`/`createEditLayout` here. The way-back link to the classic Wicket page
 * is driven by `NextMigration` (offerLegacyLink).
 */
@RestController
@RequestMapping("${Rest.URL}/configuration")
class ConfigurationEntityRest :
    AbstractDTOEntityRest<ConfigurationDO, Configuration, ConfigurationDao>(
        ConfigurationDao::class.java,
        "administration.configuration",
    ) {
    @Autowired
    private lateinit var vacationBookingConfig: TimesheetVacationBookingConfig

    override fun transformFromDB(obj: ConfigurationDO, editMode: Boolean): Configuration {
        val dto = Configuration()
        dto.copyFrom(obj)
        // The page of its own is offered only to the parameter's editors: the list opens it directly, and
        // anyone else (an admin) would only be refused there - they get the read-only view instead.
        val param = obj.parameter?.let { ConfigurationParam.ofKey(it) }
        val user = ThreadLocalUserContext.loggedInUser
        if (param == null || user == null || !baseDao.isEditor(user, param)) {
            dto.editPage = null
        }
        return dto
    }

    /**
     * Re-reads the DO from the DB by id (the parameter set is fixed, so there is always a row): parameter
     * and type come from the persisted row, never from the request, and the DTO fills only the matching
     * value slot (see [Configuration.copyTo]).
     *
     * A JSON value is stored without the whitespace between its tokens ([JsonUtils.minify]): as typed in the
     * JSON editor of the page it is indented, which would only fill the column (and the history's).
     */
    override fun transformForDB(dto: Configuration): ConfigurationDO {
        val obj = dto.id?.let { baseDao.find(it, checkAccess = false) } ?: ConfigurationDO()
        dto.copyTo(obj)
        if (obj.configurationType == ConfigurationType.JSON) {
            obj.stringValue = obj.stringValue?.let { JsonUtils.minify(it) }
        }
        return obj
    }

    /**
     * The column holds up to [ConfigurationDO.PARAM_LENGTH] characters for the structured parameters (JSON, or
     * maintained on a page of their own); a value typed on this page keeps the former limit.
     *
     * A JSON parameter is checked by the validator its owner registered (see [ConfigurationJsonValidators]).
     * The type is taken from the DB row, never from the request. The errors are given on the one value field
     * `stringValue`; each message names the part of the JSON object it is about.
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: Configuration) {
        val obj = dto.id?.let { baseDao.find(it, checkAccess = false) } ?: return
        if (obj.configurationType == ConfigurationType.JSON) {
            ConfigurationJsonValidators.validate(obj.parameter, dto.stringValue).forEach { message ->
                validationErrors.add(ValidationError(message, fieldId = "stringValue"))
            }
            // Measured as stored (see transformForDB).
            val length = dto.stringValue?.let { JsonUtils.minify(it).length } ?: 0
            if (length > ConfigurationDO.PARAM_LENGTH) {
                validationErrors.add(maxLengthError(dto, ConfigurationDO.PARAM_LENGTH))
            }
            return
        }
        val param = obj.parameter?.let { ConfigurationParam.ofKey(it) }
        validateVacationAutoBooking(validationErrors, param, dto)
        val value = dto.stringValue ?: return
        if (param?.editPage == null && value.length > ConfigurationDO.LEGACY_PARAM_LENGTH) {
            validationErrors.add(maxLengthError(dto, ConfigurationDO.LEGACY_PARAM_LENGTH))
        }
    }

    private fun maxLengthError(dto: Configuration, maxLength: Int) = ValidationError(
        translateMsg(
            "validation.error.maxLength",
            dto.i18nKey?.let { translate(it) } ?: dto.parameter,
            maxLength,
        ),
        fieldId = "stringValue",
    )

    /**
     * The days back of the first run of the vacation booking job are limited to
     * [ConfigurationService.MAX_VACATION_AUTO_BOOKING_DAYS], see
     * [org.projectforge.business.timesheet.VacationTimesheetAutoBookingJob].
     *
     * The vacation task and cost unit must exist, time sheets must be bookable on the task (not closed, project not
     * ended), and the cost unit must be bookable on the configured task. The latter is checked on saving the cost
     * unit only: switching to another pair is done by changing the task first, then the cost unit (a problem left is
     * logged by [TimesheetVacationBookingConfig.resolve]). Clearing a value is allowed.
     */
    private fun validateVacationAutoBooking(
        validationErrors: MutableList<ValidationError>,
        param: ConfigurationParam?,
        dto: Configuration,
    ) {
        when (param) {
            ConfigurationParam.TIMESHEET_VACATION_AUTO_BOOKING_DAYS -> {
                val days = dto.longValue ?: return
                if (days < 0 || days > ConfigurationService.MAX_VACATION_AUTO_BOOKING_DAYS) {
                    validationErrors.add(
                        ValidationError(
                            translateMsg(
                                "validation.error.range.integerOutOfRange",
                                0, ConfigurationService.MAX_VACATION_AUTO_BOOKING_DAYS,
                            ),
                            fieldId = "longValue",
                        )
                    )
                }
            }

            ConfigurationParam.TIMESHEET_VACATION_TASK_ID -> {
                val taskId = dto.longValue ?: return
                val result = vacationBookingConfig.check(taskId.toString(), null)
                val message = when (val problem = result.problem) {
                    TimesheetVacationBookingConfig.Problem.TASK_NOT_FOUND ->
                        translateMsg(problem.i18nKey, taskId.toString())

                    // No time sheet would be bookable: the job's runs would fail.
                    TimesheetVacationBookingConfig.Problem.TASK_NOT_BOOKABLE,
                    TimesheetVacationBookingConfig.Problem.PROJECT_ENDED ->
                        translateMsg(problem.i18nKey, result.task?.title)

                    else -> null // Fine (the cost unit is checked on saving it).
                }
                message?.let { validationErrors.add(ValidationError(it, fieldId = "longValue")) }
            }

            ConfigurationParam.TIMESHEET_VACATION_KOST2 -> {
                val kost2 = dto.stringValue?.trim()?.ifEmpty { null } ?: return
                val taskId = baseDao.getEntry(ConfigurationParam.TIMESHEET_VACATION_TASK_ID)?.longValue
                val result = vacationBookingConfig.check(taskId?.toString(), kost2)
                val message = when {
                    result.kost2 == null ->
                        translateMsg(TimesheetVacationBookingConfig.Problem.KOST2_NOT_FOUND.i18nKey, kost2)

                    result.unbookable -> translateMsg(result.problem!!.i18nKey, result.task?.title)
                    result.problem == TimesheetVacationBookingConfig.Problem.KOST2_NOT_BOOKABLE -> translateMsg(
                        TimesheetVacationBookingConfig.Problem.KOST2_NOT_BOOKABLE.i18nKey, kost2, result.task?.title,
                    )

                    else -> null // Fine, or no task configured yet.
                }
                message?.let { validationErrors.add(ValidationError(it, fieldId = "stringValue")) }
            }

            else -> {}
        }
    }

    /** The categories as pills, see [preProcessMagicFilter]. */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement(CATEGORY_FILTER, label = translate("administration.configuration.category"), defaultFilter = true)
                .buildValues(ConfigurationCategory::class.java)
        )
    }

    /**
     * Ensures every parameter of the enum has a DB row before the list is built - the next equivalent of
     * `ConfigurationListPage.init()`, so a fresh database still lists all parameters. This is the only `open`
     * hook inside the list-query pipeline (`AbstractPagesRestUtils.getList`). Runs with `checkAccess = false`
     * internally; adds no filter.
     */
    override fun preProcessMagicFilter(
        target: QueryFilter,
        source: MagicFilter,
    ): List<CustomResultFilter<ConfigurationDO>> {
        // The housekeeping below writes unchecked, so the list's read access is checked before it.
        baseDao.hasLoggedInUserSelectAccess(throwException = true)
        baseDao.checkAndUpdateDatabaseEntries()
        val filters = mutableListOf<CustomResultFilter<ConfigurationDO>>()
        // A parameter only applying with cost units configured isn't listed without (see ConfigurationParam.isCostDependent).
        if (!ConfigurationValues.instance.isCostConfigured) {
            filters.add(ParamFilter { it?.isCostDependent != true })
        }
        // The chosen categories (pills): synthetic, as the category is no property of the DO, but of its parameter.
        source.entries.find { it.field == CATEGORY_FILTER }?.let { entry ->
            entry.synthetic = true
            val categories = entry.value.values?.toSet()
            if (!categories.isNullOrEmpty()) {
                filters.add(ParamFilter { it?.category?.name in categories })
            }
        }
        // The user searches the parameter as it is *shown* - its translated label and description, runtime
        // texts no database index carries. So we take the term over, clear it from the filter (otherwise
        // MagicFilterProcessor builds a DB predicate that would drop every non-matching row before this
        // filter runs, and a CustomResultFilter can only remove rows, never add them), and match it in
        // memory over the whole (small, fixed) set - label, description and raw parameter name alike.
        val term = source.searchString?.trim()?.removeSuffix("*")?.takeIf { it.isNotBlank() }
            ?: return filters
        source.searchString = null
        filters.add(ConfigurationSearchFilter(term.lowercase()))
        return filters
    }

    /** Keeps an entry whose parameter (null if unknown) matches the given predicate. */
    private class ParamFilter(private val predicate: (ConfigurationParam?) -> Boolean) : CustomResultFilter<ConfigurationDO> {
        override fun match(list: MutableList<ConfigurationDO>, element: ConfigurationDO): Boolean =
            predicate(element.parameter?.let { ConfigurationParam.ofKey(it) })
    }

    /**
     * Keeps a parameter whose displayed label, description or raw name contains the (lowercased) search term.
     * The label/description are the same runtime translations the frontend shows; an unresolved key (a
     * parameter with no description) translates back to itself, which [resolvedText] drops so it never
     * matches as noise. See [preProcessMagicFilter].
     */
    private class ConfigurationSearchFilter(private val needle: String) : CustomResultFilter<ConfigurationDO> {
        override fun match(list: MutableList<ConfigurationDO>, element: ConfigurationDO): Boolean {
            val haystacks = listOfNotNull(
                resolvedText(element.i18nKey),
                resolvedText(element.descriptionI18nKey),
                element.parameter,
                element.parameter?.let { ConfigurationParam.ofKey(it) }?.category?.let { resolvedText(it.i18nKey) },
            )
            return haystacks.any { it.lowercase().contains(needle) }
        }

        /** The translation of [key], or null when the key is unknown (translate returns the key unchanged). */
        private fun resolvedText(key: String?): String? {
            key ?: return null
            val text = translate(key)
            return if (text == key) null else text
        }
    }

    /**
     * Sorts the "parameter" list column by the parameter's *displayed* label rather than by its raw name:
     * the label is the translation of the parameter's i18n key in the user's locale (the same text the
     * frontend shows and [Configuration.label] carries), so the order matches what the eye reads, not the
     * variable name behind it. Applied in memory over the whole (small, fixed) result set - the label is a
     * runtime translation no database `ORDER BY` can express. See [computedSortProperties].
     */
    override val computedSortProperties: Map<String, (ConfigurationDO) -> Comparable<*>?>
        get() = mapOf(
            "parameter" to { config -> translate(config.i18nKey).lowercase() },
            // By the category, within a category by the label (one comparable key, as the in-memory sort takes one).
            "category" to { config ->
                val category = config.parameter?.let { ConfigurationParam.ofKey(it) }?.category
                "${category?.let { translate(it.i18nKey) } ?: ""}\u0000${translate(config.i18nKey)}".lowercase()
            },
        )

    companion object {
        /** The filter field of the category pills, see [addMagicFilterElements]. */
        private const val CATEGORY_FILTER = "category"
    }
}
