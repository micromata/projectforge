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

import org.projectforge.framework.configuration.ConfigurationDao
import org.projectforge.framework.configuration.ConfigurationJsonValidators
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.ConfigurationType
import org.projectforge.framework.configuration.entities.ConfigurationDO
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Configuration
import org.projectforge.ui.ValidationError
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
     */
    override fun transformForDB(dto: Configuration): ConfigurationDO {
        val obj = dto.id?.let { baseDao.find(it, checkAccess = false) } ?: ConfigurationDO()
        dto.copyTo(obj)
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
            return
        }
        val param = obj.parameter?.let { ConfigurationParam.ofKey(it) }
        val value = dto.stringValue ?: return
        if (param?.editPage == null && value.length > ConfigurationDO.LEGACY_PARAM_LENGTH) {
            validationErrors.add(
                ValidationError(
                    translateMsg(
                        "validation.error.maxLength",
                        dto.i18nKey?.let { translate(it) } ?: dto.parameter,
                        ConfigurationDO.LEGACY_PARAM_LENGTH,
                    ),
                    fieldId = "stringValue",
                )
            )
        }
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
        baseDao.checkAndUpdateDatabaseEntries()
        // The user searches the parameter as it is *shown* - its translated label and description, runtime
        // texts no database index carries. So we take the term over, clear it from the filter (otherwise
        // MagicFilterProcessor builds a DB predicate that would drop every non-matching row before this
        // filter runs, and a CustomResultFilter can only remove rows, never add them), and match it in
        // memory over the whole (small, fixed) set - label, description and raw parameter name alike.
        val term = source.searchString?.trim()?.removeSuffix("*")?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        source.searchString = null
        return listOf(ConfigurationSearchFilter(term.lowercase()))
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
            "parameter" to { config -> translate(config.i18nKey).lowercase() }
        )
}
