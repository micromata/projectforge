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


package org.projectforge.jira

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.configuration.ConfigurationJsonValues
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.configuration.ConfigureJiraServer
import org.projectforge.framework.json.JsonUtils

private val log = KotlinLogging.logger {}

/**
 * The JIRA servers issue keys are linked to, stored as JSON in the configuration parameter `ConfigurationParam.JIRA`,
 * e.g. `{"browseBaseUrl":"https://jira.acme.com/jira/browse/",
 * "servers":[{"baseUrl":"https://customer.acme.com/jira/browse/","projects":"ACME, ADMIN"}]}`.
 */
class JiraConfig(
    /**
     * Base url for linking JIRA issues of projects not hosted on one of the [servers]:
     * https://jira.acme.com/jira/browse/ (don't forget the closing '/'). The issue key UPPERCASE_LETTERS-### is
     * appended to it.
     */
    var browseBaseUrl: String? = null,
    /** Servers by the projects hosted on them. */
    var servers: List<ConfigureJiraServer>? = null,
) {
    /** True if a browse base url or a server is given. */
    @get:JsonIgnore
    val isConfigured: Boolean
        get() = !browseBaseUrl.isNullOrBlank() || !servers.isNullOrEmpty()

    companion object {
        private var cachedJson: String? = null
        private var cachedConfig: JiraConfig? = null

        /** The current settings (configuration parameter `jira`), cached as long as the parameter isn't changed. */
        @JvmStatic
        @Synchronized
        fun get(): JiraConfig {
            val json = ConfigurationJsonValues.get(ConfigurationParam.JIRA)
            cachedConfig?.let { if (json == cachedJson) return it }
            return parse(json).also {
                cachedConfig = it
                cachedJson = json
            }
        }

        /** The settings of the given JSON value, none if it is blank or not readable. */
        fun parse(json: String?): JiraConfig {
            if (json.isNullOrBlank()) {
                return JiraConfig()
            }
            return try {
                JsonUtils.fromJson(json, JiraConfig::class.java, failOnUnknownProps = false) ?: JiraConfig()
            } catch (ex: Exception) {
                log.warn { "Jira: configuration (jira) not readable, no issues are linked: ${ex.message}" }
                JiraConfig()
            }
        }
    }
}
