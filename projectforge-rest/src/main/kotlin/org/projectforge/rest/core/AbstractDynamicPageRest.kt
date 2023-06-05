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

package org.projectforge.rest.core

import mu.KotlinLogging
import org.projectforge.business.configuration.DomainService
import org.projectforge.framework.i18n.I18nKeyAndParams
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.ServerData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

/**
 * For single dynamic React pages. Use this class especially for CSRF protection.
 */
abstract class AbstractDynamicPageRest {
  @Autowired
  protected lateinit var sessionCsrfService: SessionCsrfService

  @Autowired
  protected lateinit var domainService: DomainService

  /**
   * Creates new server data object with csrfToken.
   */
  protected fun createServerData(request: HttpServletRequest): ServerData {
    return sessionCsrfService.createServerData(request)
  }

  protected fun validateCsrfToken(request: HttpServletRequest, postData: PostData<*>): ResponseEntity<ResponseAction>? {
    return sessionCsrfService.validateCsrfToken(request, postData)
  }

  protected fun processErrorKeys(errorMsgKeys: List<I18nKeyAndParams>?): ResponseEntity<ResponseAction>? {
    if (errorMsgKeys.isNullOrEmpty()) {
      return null
    }
    val validationErrors = errorMsgKeys.map { ValidationError.create(it) }
    return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
  }

  protected fun getUrl(path: String): String {
    return domainService.getDomain(path)
  }

  /**
   * Relative rest path (without leading /rs
   */
  fun getRestPath(subPath: String? = null): String {
    return RestResolver.getRestUrl(this::class.java, subPath, true)
  }

  /**
   * Relative rest path (without leading /rs
   */
  fun getRestRootPath(subPath: String? = null): String {
    return getRestPath(subPath)
  }

  companion object {
    fun createValidationErrors(vararg errors: ValidationError): MutableList<ValidationError> {
      val validationErrors = mutableListOf<ValidationError>()
      errors.forEach { validationErrors.add(it) }
      return validationErrors
    }

    fun showValidationErrors(vararg errors: ValidationError): ResponseEntity<ResponseAction> {
      val validationErrors = createValidationErrors(*errors)
      return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
    }
  }
}
