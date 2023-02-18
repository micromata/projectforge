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

package org.projectforge.rest.sipgate

import mu.KotlinLogging
import org.projectforge.business.sipgate.SipgateDevice
import org.projectforge.business.sipgate.SipgateIoLogsResponse
import org.projectforge.business.sipgate.SipgateUser
import org.projectforge.common.StringHelper
import org.projectforge.framework.json.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class SipgateService {
  class NewCallRequest(var deviceId: String, var caller: String, var callee: String, var callerId: String) {
    override fun toString(): String {
      val anonymized = StringHelper.hideStringEnding(callee, 'x', 5)
      return "deviceId='$deviceId', caller='$caller', callerId='$callerId', callee='$anonymized'"
    }
  }

  class SendWebSmsRequest(var smsId: String, var recipient: String, var message: String) {
    var sendAt: Int? = null
    override fun toString(): String {
      val anonymizedRecipient = StringHelper.hideStringEnding(recipient, 'x', 5)
      val messageStart = message.take(3)
      return "smsId='$smsId', recipient='$anonymizedRecipient', message='$messageStart...'"
    }
  }

  @Autowired
  internal lateinit var sipgateClient: SipgateClient

  protected lateinit var webClient: WebClient

  @PostConstruct
  internal fun postConstruct() {
    webClient = sipgateClient.webClient
  }


  /**
   * Initiates a new call: /sessions/calls
   */
  fun getLogs(): List<SipgateIoLogsResponse?>? {
    val path = "/log/webhooks"
    try {
      val uriSpec = webClient.get()
      log.info { "Trying to initiate call '$path'..." }
      val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
        uriBuilder.path(path).build()
      }
      val response = sipgateClient.execute(headersSpec, String::class.java)
      val list = JsonUtils.fromJson(response, LogsListData::class.java)
      return list?.items
    } catch (ex: Exception) {
      log.error { "Error while initiating call for '$path': ${ex.message}" }
      return null
    }
  }

  /**
   * Initiates a new call: /users
   */
  fun getUsers(): List<SipgateUser>? {
    val path = "/users"
    try {
      val uriSpec = webClient.get()
      log.info { "Trying to initiate call '$path'..." }
      val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
        uriBuilder.path(path).build()
      }
      val response = sipgateClient.execute(headersSpec, String::class.java)
      val list = JsonUtils.fromJson(response, UserListData::class.java)
      return list?.items
    } catch (ex: Exception) {
      log.error { "Error while initiating call for '$path': ${ex.message}" }
      return null
    }
  }

  /**
   * Initiates a new call: /devices
   */
  fun getDevices(user: SipgateUser): List<SipgateDevice>? {
    val path = "/${user.id}/devices"
    try {
      val uriSpec = webClient.get()
      log.info { "Trying to initiate call '$path'..." }
      val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
        uriBuilder.path(path).build()
      }
      val response = sipgateClient.execute(headersSpec, String::class.java)
      val list = JsonUtils.fromJson(response, DeviceListData::class.java)
      return list?.items
    } catch (ex: Exception) {
      log.error { "Error while initiating call for '$path': ${ex.message}" }
      return null
    }
  }

  /**
   * Initiates a new call: /sessions/calls
   */
  fun initCall(deviceId: String, caller: String, callee: String, callerId: String): Boolean {
    val newCallRequest = NewCallRequest(deviceId = deviceId, caller = caller, callee = callee, callerId = callerId)
    val json = JsonUtils.toJson(newCallRequest)
    val anonymized = StringHelper.hideStringEnding(callee, 'x', 5)
    try {
      val uriSpec = webClient.post()
      log.info { "Trying to initiate call for $newCallRequest." }
      val bodySpec = uriSpec.uri("/sessions/calls")
        .body(
          BodyInserters.fromValue(json)
        )
      val response = sipgateClient.execute(bodySpec, String::class.java, HttpStatus.CREATED)
      return true
    } catch (ex: Exception) {
      log.error { "Error while initiating call for $newCallRequest: ${ex.message}" }
      return false
    }
  }

  /**
   * Initiates a new sms: /sessions/sms
   */
  fun sendSms(smsId: String, recipient: String, message: String): Boolean {
    val smsRequest = SendWebSmsRequest(smsId = smsId, recipient = recipient, message = message)
    val json = JsonUtils.toJson(smsRequest)
    try {
      val uriSpec = webClient.post()
      log.info { "Trying to send sms $smsRequest" }
      val bodySpec = uriSpec.uri("/sessions/sms")
        .body(
          BodyInserters.fromValue(json)
        )
      val response = sipgateClient.execute(bodySpec, String::class.java, HttpStatus.CREATED)
      return true
    } catch (ex: Exception) {
      log.error { "Error while sending sms $smsRequest: ${ex.message}" }
      return false
    }
  }
}
