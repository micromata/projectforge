/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.messaging.SmsSender
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@Service
class My2FARestService {
  @Autowired
  private lateinit var smsSenderConfig: SmsSenderConfig

  private lateinit var smsSender: SmsSender

  val smsConfigured
    get() = SystemStatus.isDevelopmentMode() || smsSenderConfig.isSmsConfigured()

  /**
   * Creates a OTP (10 minutes valid) in [ExpiringSessionAttributes] and sends the code to the mobile number.
   */
  fun createAndSendOTP(request: HttpServletRequest, mobilePhone: String): SmsSender.HttpResponseCode {
    val code = NumberHelper.getSecureRandomDigits(6)
    ExpiringSessionAttributes.setAttribute(request.getSession(false), SESSSION_ATTRIBUTE, code, 10)
    val msg = "Code: $code ${translate("address.sendSms.doNotReply")}"
    if (SystemStatus.isDevelopmentMode()) {
      log.info { "Text message would be sent in production mode to '$mobilePhone': $msg" }
      return HttpResponseCode.SUCCESS
    }
    return smsSender.send(mobilePhone, msg)
  }

  fun getResultMessage(responseCode: HttpResponseCode, number: String): String {
    val errorKey = smsSender.getErrorMessage(responseCode)
    if (errorKey != null) {
      return translate(errorKey)
    }
    return translateMsg("address.sendSms.sendMessage.result.successful", number, PFDateTime.now().format())
  }

  /**
   * Checks the code entered by the user by getting the code created before via [createAndSendOTP].
   */
  fun checkOTP(request: HttpServletRequest, code: String): Boolean {
    val sessionCode =
      ExpiringSessionAttributes.getAttribute(request.getSession(false), SESSSION_ATTRIBUTE) ?: return false
    return sessionCode == code
  }

  @PostConstruct
  fun postConstruct() {
    smsSender = SmsSender(smsSenderConfig)
  }

  companion object {
    private const val SESSSION_ATTRIBUTE = "My2FARestService.otp"
  }
}
