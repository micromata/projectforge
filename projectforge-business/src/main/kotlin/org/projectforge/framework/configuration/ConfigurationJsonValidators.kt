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

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.framework.i18n.translate
import java.util.concurrent.ConcurrentHashMap

/**
 * The validators of the configuration parameters of type [ConfigurationType.JSON], by parameter key. A service
 * owning such a parameter registers its validator (e.g. in its `@PostConstruct`); the configuration edit page
 * calls [validate] before saving. A parameter without a validator is checked for well-formed JSON only.
 */
object ConfigurationJsonValidators {
  /** Returns the translated error messages of the given JSON value, empty if it is valid. */
  fun interface Validator {
    fun validate(json: String): List<String>
  }

  private val validators = ConcurrentHashMap<String, Validator>()

  private val objectMapper = ObjectMapper()

  fun register(param: IConfigurationParam, validator: Validator) {
    validators[param.key] = validator
  }

  /** The translated error messages of the [json] value of the parameter with the given key. Blank is valid. */
  fun validate(parameter: String?, json: String?): List<String> {
    if (json.isNullOrBlank()) {
      return emptyList()
    }
    try {
      objectMapper.readTree(json)
    } catch (ex: Exception) {
      return listOf(translate("administration.configuration.json.invalid"))
    }
    val validator = parameter?.let { validators[it] } ?: return emptyList()
    return validator.validate(json)
  }
}
