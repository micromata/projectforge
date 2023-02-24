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
import org.projectforge.business.sipgate.*
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
  /**
   * DeviceId is only required if the caller parameter is a phone number and not a deviceId itself.
   * Use callerId to set a custom number that will be displayed to the callee.
   */
  class NewCallRequest(var deviceId: String? = null, var caller: String, var callee: String, var callerId: String) {
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
   * Initiates a new call: /log/webhooks
   */
  fun getLogs(): List<SipgateIoLogsResponse?> {
    return getEntities("/log/webhooks", "LogsResponse", LogsListData::class.java)
  }

  /**
   * Initiates a new call: /users
   */
  fun getUsers(): List<SipgateUser> {
    return getEntities("/users", "User", UserListData::class.java)
  }

  /**
   * Initiates a new call: /devices
   */
  fun getDevices(user: SipgateUser): List<SipgateDevice> {
    return getEntities("/${user.id}/devices", "Device", DeviceListData::class.java)
  }

  /**
   * Initiates a new call: /numbers
   */
  fun getNumbers(): List<SipgateNumber> {
    return getEntities("/numbers", "Number", NumberListData::class.java)
  }

  /**
   * Initiates a new call: /addresses
   */
  fun getAddresses(): List<SipgateAddress> {
    return getEntities("/addresses", "Address", AddressListData::class.java)
  }

  private fun <T> getEntities(
    path: String,
    entityName: String,
    listDataClass: Class<out ListData<T>>,
    offset: Int = 0,
    limit: Int = 5000,
    maxNumberOfPages: Int = 100,
    debugConsoleOutForTesting: Boolean = false,
  ): List<T> {
    return getList(
      webClient,
      sipgateClient,
      path = path,
      entityName = entityName,
      offset = offset,
      limit = limit,
      maxNumberOfPages = maxNumberOfPages,
      debugConsoleOutForTesting = debugConsoleOutForTesting,
      listDataClass = listDataClass,
    )
  }

  /**
   * Initiates a new call: /sessions/calls
   * DeviceId is only required if the caller parameter is a phone number and not a deviceId itself.
   * Use callerId to set a custom number that will be displayed to the callee.
   */
  fun initCall(deviceId: String?, caller: String, callee: String, callerId: String): Boolean {
    val newCallRequest = NewCallRequest(deviceId = deviceId, caller = caller, callee = callee, callerId = callerId)
    val json = JsonUtils.toJson(newCallRequest, ignoreNullableProps = true)
    try {
      val uriSpec = webClient.post()
      log.info { "Trying to initiate call for $newCallRequest." }
      val bodySpec = uriSpec.uri("/sessions/calls")
        .body(
          BodyInserters.fromValue(json)
        )
      sipgateClient.execute(bodySpec, String::class.java, HttpStatus.OK)
      // response: {"sessionId":"52576B150..."}
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
      sipgateClient.execute(bodySpec, String::class.java, HttpStatus.NO_CONTENT)
      return true
    } catch (ex: Exception) {
      log.error { "Error while sending sms $smsRequest: ${ex.message}" }
      return false
    }
  }

  companion object {
    internal fun <T> getList(
      webClient: WebClient,
      sipgateClient: SipgateClient,
      path: String,
      entityName: String,
      listDataClass: Class<out ListData<T>>,
      offset: Int = 0,
      limit: Int = 5000,
      maxNumberOfPages: Int = 100,
      debugConsoleOutForTesting: Boolean = false,
    ): List<T> {
      try {
        // Parameters: limit=<pagesize>, offset=<page>
        val result = mutableListOf<T>()
        var pageCounter = 0
        var currentOffset = offset
        while (pageCounter++ < maxNumberOfPages) {
          val uriSpec = webClient.get()
          val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
            uriBuilder
              .path(path)
              .queryParam("offset", currentOffset)
              .queryParam("limit", limit)
              .build()
          }
          val response = sipgateClient.execute(headersSpec, String::class.java)
          if (debugConsoleOutForTesting) {
            println("response: $response")
          }
          val data = JsonUtils.fromJson(response, listDataClass, failOnUnknownProps = false)
          val entities = data?.items ?: break
          val totalCount = data.totalCount ?: 0
          result.addAll(entities)
          currentOffset += limit
          if (currentOffset > totalCount) {
            break
          }
        }
        log.info { "Got ${result.size} entries of $entityName from Sipgate." }
        return result
      } catch (ex: Exception) {
        log.error("Can't read entries of type $entityName (may-be no access): ${ex.message}", ex)
        return mutableListOf()
      }
    }
  }
}
