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


package org.projectforge.framework.configuration

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.projectforge.framework.xmlstream.XmlField
import org.projectforge.framework.xmlstream.XmlObject
import org.projectforge.framework.xmlstream.XmlOmitField

/**
 * A jira server hosting the given projects (part of the configuration parameter jira, see
 * [org.projectforge.jira.JiraConfig]). The xml annotations are only used for reading the former config.xml.
 *
 * @author Kai Reinhard
 */
@XmlObject(alias = "jiraServer")
@JsonAutoDetect(
  fieldVisibility = JsonAutoDetect.Visibility.ANY,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  isGetterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE,
)
class ConfigureJiraServer {
  /**
   * @return jira base url for projects.
   */
  @XmlField(asAttribute = true, alias = "browse-base-url")
  var baseUrl: String? = null

  /**
   * @return List of projects hosted on this server (csv, e. g. "ACME;ADMIN;SUPPORT;PORTAL".
   */
  @XmlField(asAttribute = true, alias = "projects")
  @field:JsonProperty("projects")
  var projectsAsString: String? = null

  /**
   * The projects of [projectsAsString] (upper case), null if none is given.
   */
  val projects: Array<String>?
    get() {
      if (!initialized) {
        initialized = true
        parsedProjects = projectsAsString?.split(";", ",")
          ?.map { it.trim().uppercase() }
          ?.filter { it.isNotBlank() }
          ?.takeIf { it.isNotEmpty() }
          ?.toTypedArray()
      }
      return parsedProjects
    }

  @XmlOmitField
  @Transient
  private var parsedProjects: Array<String>? = null

  @XmlOmitField
  @Transient
  private var initialized = false

  override fun toString(): String {
    val builder = ReflectionToStringBuilder(this)
    return builder.toString()
  }
}
