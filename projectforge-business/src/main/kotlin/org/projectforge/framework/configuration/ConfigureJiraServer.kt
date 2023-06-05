/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.projectforge.framework.xmlstream.XmlField
import org.projectforge.framework.xmlstream.XmlObject
import org.projectforge.framework.xmlstream.XmlOmitField

/**
 * Used in config.xml for defining jira server urls (by space names).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XmlObject(alias = "jiraServer")
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
  var projectsAsString: String? = null

  @XmlOmitField
  var projects: Array<String>? = null
    get() {
      if (!intialized) {
        intialized = true
        projectsAsString.let { value ->
          if (value.isNullOrBlank()) {
            projects = null
          } else {
            val list = mutableListOf<String>()
            value.split(";", ",").forEach {
              val project = it.trim().uppercase()
              if (project.isNotBlank()) {
                list.add(project)
              }
            }
            if (list.isNotEmpty()) {
              projects = list.toTypedArray()
            }
          }
        }
      }
      return field
    }
    private set

  private var intialized = false

  override fun toString(): String {
    val builder = ReflectionToStringBuilder(this)
    return builder.toString()
  }
}
