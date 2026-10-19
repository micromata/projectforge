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

package org.projectforge.business.group

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.json.JsonUtils

private val log = KotlinLogging.logger {}

/**
 * The naming convention of groups, stored as JSON in the configuration parameter
 * `ConfigurationParam.GROUP_NOMENCLATURE` and edited as a whole on the configuration page.
 *
 * Only a description for now: the wizards suggest group names following [pattern] and the suffixes and show
 * [description], but no name is validated against it. Without any setting the wizards keep their former
 * suggestion, the project's (or element's) name plus the translated role suffix.
 */
class GroupNomenclatureConfig(
    /** Rich text (HTML) explaining the convention, shown where groups are named. */
    var description: String? = null,
    /**
     * The base name of a project's groups with the placeholders `{customer.identifier}`, `{customer.name}`,
     * `{customer.number}`, `{project.identifier}`, `{project.name}` and `{project.number}`; an empty identifier
     * falls back to the name. Null or blank: the project's name.
     */
    var pattern: String? = null,
    /** Appended with "-" to the name of the manager group; null: the translated default ("pm"). */
    var managerSuffix: String? = null,
    /** Appended with "-" to the name of the team group; null: none. */
    var teamSuffix: String? = null,
    /** Appended with "-" to the name of the group of externals; null: the translated default. */
    var externalSuffix: String? = null,
    /**
     * The case of the whole suggested name, suffix included: [LETTER_CASE_LOWER], [LETTER_CASE_UPPER], or
     * null (or anything else) to keep it as built.
     */
    var letterCase: String? = null,
) {
    companion object {
        const val LETTER_CASE_LOWER = "LOWER"
        const val LETTER_CASE_UPPER = "UPPER"

        /** The current settings (configuration parameter `groupNomenclature`). */
        fun get(): GroupNomenclatureConfig {
            return parse(Configuration.instance.getStringValue(ConfigurationParam.GROUP_NOMENCLATURE))
        }

        /** The settings of the given JSON value, the defaults if it is blank or not readable. */
        fun parse(json: String?): GroupNomenclatureConfig {
            if (json.isNullOrBlank()) {
                return GroupNomenclatureConfig()
            }
            return try {
                JsonUtils.fromJson(json, GroupNomenclatureConfig::class.java, failOnUnknownProps = false)
                    ?: GroupNomenclatureConfig()
            } catch (ex: Exception) {
                log.warn { "Group nomenclature: configuration not readable, using the defaults: ${ex.message}" }
                GroupNomenclatureConfig()
            }
        }
    }
}
