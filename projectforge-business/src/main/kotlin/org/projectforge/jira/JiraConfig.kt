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
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.json.JsonUtils
import java.net.URI

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

        private val PROJECT_REGEX = Regex("[A-Z][A-Z_0-9]*")

        /**
         * The errors of the given settings (i18n), empty if they are valid: the urls must be http(s) urls, every
         * server needs a url and projects, and a project key is hosted on one server only.
         */
        fun validate(config: JiraConfig): List<String> {
            val errors = mutableListOf<String>()
            config.browseBaseUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
                if (!isHttpUrl(url)) {
                    errors.add(translateMsg("administration.configuration.jira.error.browseBaseUrl", url))
                }
            }
            val projectServers = mutableMapOf<String, Int>()
            config.servers?.forEachIndexed { index, server ->
                val no = index + 1
                val url = server.baseUrl?.trim()
                if (url.isNullOrEmpty()) {
                    errors.add(translateMsg("administration.configuration.jira.error.serverUrlMissing", no))
                } else if (!isHttpUrl(url)) {
                    errors.add(translateMsg("administration.configuration.jira.error.serverUrl", no, url))
                }
                val projects = server.projects
                if (projects.isNullOrEmpty()) {
                    errors.add(translateMsg("administration.configuration.jira.error.projectsMissing", no))
                }
                projects?.forEach { project ->
                    if (!PROJECT_REGEX.matches(project)) {
                        errors.add(translateMsg("administration.configuration.jira.error.project", no, project))
                    } else {
                        val other = projectServers.putIfAbsent(project, no)
                        if (other != null && other != no) {
                            errors.add(translateMsg("administration.configuration.jira.error.duplicate", project, other, no))
                        }
                    }
                }
            }
            return errors
        }

        private fun isHttpUrl(url: String): Boolean {
            return try {
                val uri = URI(url)
                (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
            } catch (_: Exception) {
                false
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
