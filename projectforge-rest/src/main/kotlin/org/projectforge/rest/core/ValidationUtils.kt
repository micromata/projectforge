/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.apache.commons.beanutils.NestedNullException
import org.apache.commons.beanutils.PropertyUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.ui.ElementsRegistry
import org.projectforge.ui.ValidationError

private val log = KotlinLogging.logger {}

/**
 * Utility functions for validation in REST endpoints.
 */
object ValidationUtils {
    /**
     * Validates required fields of an object based on metadata from ElementsRegistry.
     * Checks all properties marked as required in @PropertyInfo annotations.
     *
     * @param obj The object to validate (typically a DO or DTO)
     * @return List of validation errors for missing required fields
     */
    fun validateRequiredFields(obj: Any): MutableList<ValidationError> {
        val validationErrors = mutableListOf<ValidationError>()
        val propertiesMap = ElementsRegistry.getProperties(obj::class.java)
        if (propertiesMap.isNullOrEmpty()) {
            log.error("Internal error, can't find propertiesMap for '${obj::class.java}' in ElementsRegistry. No validation errors will be built automatically.")
            return validationErrors
        }
        propertiesMap.forEach {
            val property = it.key
            val elementInfo = it.value
            val value =
                try {
                    PropertyUtils.getProperty(obj, property)
                } catch (ex: NestedNullException) {
                    null
                } catch (ex: Exception) {
                    log.warn("Unknown property '${obj::class.java}.$property': ${ex.message}.")
                    null
                }
            if (elementInfo.required == true) {
                var error = false
                if (value == null) {
                    error = true
                } else {
                    when (value) {
                        is String -> {
                            if (value.isBlank()) {
                                error = true
                            }
                        }
                    }
                }
                if (error)
                    validationErrors.add(
                        ValidationError(
                            translateMsg("validation.error.fieldRequired", translate(elementInfo.i18nKey)),
                            fieldId = property, messageId = elementInfo.i18nKey
                        )
                    )
            }
        }
        return validationErrors
    }
}
