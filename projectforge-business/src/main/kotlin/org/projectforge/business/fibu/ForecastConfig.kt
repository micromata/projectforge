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

package org.projectforge.business.fibu

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam
import org.projectforge.framework.json.JsonUtils

private val log = KotlinLogging.logger {}

/**
 * The settings of the forecast, stored as JSON in the configuration parameter `ConfigurationParam.FIBU_FORECAST`
 * and edited as a whole on the configuration page. Further forecast settings are added as fields here.
 */
class ForecastConfig(
  /**
   * Rich text (HTML) of finance and controlling shown in the tooltip of the planning date of the forecast
   * statistics, e.g. which planning date to choose for which year.
   */
  var planningDateHint: String? = null,
) {
  companion object {
    /** The current settings (configuration parameter `fibu.forecast`). */
    fun get(): ForecastConfig {
      return parse(Configuration.instance.getStringValue(ConfigurationParam.FIBU_FORECAST))
    }

    /** The settings of the given JSON value, the defaults if it is blank or not readable. */
    fun parse(json: String?): ForecastConfig {
      if (json.isNullOrBlank()) {
        return ForecastConfig()
      }
      return try {
        JsonUtils.fromJson(json, ForecastConfig::class.java, failOnUnknownProps = false) ?: ForecastConfig()
      } catch (ex: Exception) {
        log.warn { "Forecast: configuration not readable, using the defaults: ${ex.message}" }
        ForecastConfig()
      }
    }
  }
}
