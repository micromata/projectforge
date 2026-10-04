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

package org.projectforge

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.persistence.database.DatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct


@Component
class SystemStatus {
  @Autowired
  private lateinit var configurationService: ConfigurationService

  @Autowired
  private lateinit var databaseService: DatabaseService

  val appname = ProjectForgeVersion.APP_ID
  val version = ProjectForgeVersion.VERSION_STRING
  val buildTimestamp = ProjectForgeVersion.BUILD_TIMESTAMP
  val buildDate = ProjectForgeVersion.BUILD_DATE
  val releaseYear = ProjectForgeVersion.YEAR
  val scmIdFull = ProjectForgeVersion.SCM_ID_FULL
  val scmId = ProjectForgeVersion.SCM_ID
  var messageOfTheDay: String? = null
    private set
  var logoFile: String? = null
    private set
  val copyRightYears = ProjectForgeVersion.COPYRIGHT_YEARS

  @Value("\${projectforge.development.mode}")
  var developmentMode: Boolean = false
    private set

  /**
   * `projectforge.testsystemMode`: this instance is a test system (e.g. a copy of the productive data), marked
   * in the client in [testsystemColor], as Wicket coloured its pages' background.
   */
  @Value("\${projectforge.testsystemMode:false}")
  var testsystemMode: Boolean = false
    private set

  /** `projectforge.testsystemColor`, a CSS colour; anything else falls back to the default (see [testsystemColor]). */
  @Value("\${projectforge.testsystemColor:$DEFAULT_TESTSYSTEM_COLOR}")
  private var testsystemColorProperty: String? = null

  /** The configured colour of the test-system marker, only if it is a plain CSS colour (it ends up in a style). */
  val testsystemColor: String
    get() = sanitizeColor(testsystemColorProperty)

  var setupRequiredFirst: Boolean? = null
  var updateRequiredFirst: Boolean? = null

  /**
   * This flag is set to true if ProjectForge's start is completed.
   */
  var upAndRunning: Boolean = false
  val startTimeMillis: Long = System.currentTimeMillis()

  @PostConstruct
  private fun postConstruct() {
    messageOfTheDay = Configuration.instance
      .getStringValue(ConfigurationParam.MESSAGE_OF_THE_DAY)
    logoFile = configurationService.syntheticLogoName
    if (!databaseService.databaseTablesWithEntriesExist())
      setupRequiredFirst = true
    devMode = developmentMode
  }

  companion object {
    private var devMode: Boolean? = null

    internal const val DEFAULT_TESTSYSTEM_COLOR = "#ff6868"

    private val COLOR_REGEX = Regex("^(#[0-9a-fA-F]{3,8}|[a-zA-Z]{3,30})$")

    /** A hex colour or a colour name; anything else (a style injection, a typo) is the default. */
    internal fun sanitizeColor(color: String?): String =
      color?.trim()?.takeIf { COLOR_REGEX.matches(it) } ?: DEFAULT_TESTSYSTEM_COLOR

    @JvmStatic
    fun isDevelopmentMode(): Boolean {
      return devMode == true
    }

    fun internalSet4JunitTests(developmentMode: Boolean) {
      devMode = developmentMode
    }
  }
}

