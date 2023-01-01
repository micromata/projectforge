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

package org.projectforge.messaging

import mu.KotlinLogging
import org.apache.commons.collections4.MapUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.Consts
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.projectforge.common.StringHelper
import org.projectforge.sms.SmsSenderConfig
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {}

class SmsSender(private var config: SmsSenderConfig) {
  enum class HttpResponseCode {
    SUCCESS, NUMBER_ERROR, MESSAGE_TO_LARGE, MESSAGE_ERROR, UNKNOWN_ERROR
  }

  /**
   * Variables #message and #number will be replaced in url as well as in parameter values.
   *
   * @return
   */
  fun send(phoneNumber: String?, message: String?): HttpResponseCode {
    if (message == null || phoneNumber == null) {
      log.error(
        "Failed to send message to destination number: '" + StringHelper.hideStringEnding(phoneNumber, 'x', 3)
            + ". Message is null!"
      )
      return HttpResponseCode.MESSAGE_ERROR
    }
    if (message.length > config.smsMaxMessageLength) {
      log.error(
        "Failed to send message to destination number: '" + StringHelper.hideStringEnding(phoneNumber, 'x', 3)
            + ". Message is to large, max length is " + config.smsMaxMessageLength + ", but current message size is " + message.length
      )
      return HttpResponseCode.MESSAGE_TO_LARGE
    }
    val proceededUrl = replaceVariables(config.url, phoneNumber, message, true)
    val method = createHttpMethod(proceededUrl, phoneNumber, message)
    createHttpClient().use { client ->
      return try {
        client.execute(method).use { httpResponse ->
          val statusCode = httpResponse.statusLine.statusCode
          var response: String? = null
          if (statusCode == HttpStatus.SC_OK) {
            val stream = httpResponse.entity.content
            response = IOUtils.toString(stream, StandardCharsets.UTF_8)
          }
          log.info(
            "Tried to send message to destination number: '" + StringHelper.hideStringEnding(phoneNumber, 'x', 3)
                + ". Response from service: " + response
          )
          val responseCode = if (response == null) {
            HttpResponseCode.UNKNOWN_ERROR
          } else if (matches(response, config.smsReturnPatternNumberError)) {
            HttpResponseCode.NUMBER_ERROR
          } else if (matches(response, config.smsReturnPatternMessageToLargeError)) {
            HttpResponseCode.MESSAGE_TO_LARGE
          } else if (matches(response, config.smsReturnPatternMessageError)) {
            HttpResponseCode.MESSAGE_ERROR
          } else if (matches(response, config.smsReturnPatternError)) {
            HttpResponseCode.UNKNOWN_ERROR
          } else if (matches(response, config.smsReturnPatternSuccess)) {
            HttpResponseCode.SUCCESS
          } else {
            HttpResponseCode.UNKNOWN_ERROR
          }
          if (responseCode != HttpResponseCode.SUCCESS) {
            log.error("Unexpected response from sms gateway: $statusCode: $response (if this call was successful, did you configured projectforge.sms.returnCodePattern.success?).")
          }
          responseCode
        }
      } catch (ex: IOException) {
        val errorKey = "Call failed. Please contact administrator."
        log.error(
          errorKey + ": " + proceededUrl + " for number "
              + StringHelper.hideStringEnding(phoneNumber, 'x', 3)
        )
        HttpResponseCode.UNKNOWN_ERROR
      }
    }
  }

  fun getErrorMessage(responseCode: HttpResponseCode?): String? {
    return if (responseCode == null) {
      "address.sendSms.sendMessage.result.unknownError"
    } else if (responseCode == HttpResponseCode.SUCCESS) {
      null
    } else if (responseCode == HttpResponseCode.MESSAGE_ERROR) {
      "address.sendSms.sendMessage.result.messageError"
    } else if (responseCode == HttpResponseCode.NUMBER_ERROR) {
      "address.sendSms.sendMessage.result.wrongOrMissingNumber"
    } else if (responseCode == HttpResponseCode.MESSAGE_TO_LARGE) {
      "address.sendSms.sendMessage.result.messageToLarge"
    } else {
      "address.sendSms.sendMessage.result.unknownError"
    }
  }

  private fun matches(str: String?, regexp: String?): Boolean {
    return if (regexp == null || str == null) {
      false
    } else str.matches(regexp.toRegex())
  }

  /**
   * Variables #number and #message will be replaced by the user's form input.
   *
   * @param str    The string to proceed.
   * @param number The extracted phone number (already preprocessed...)
   * @return The given str with replaced vars (if exists).
   */
  private fun replaceVariables(str: String?, number: String?, message: String, urlEncode: Boolean): String? {
    var str = str
    if (number == null) return ""
    str = StringUtils.replaceOnce(str, "#number", if (urlEncode) encode(number) else number)
    str = StringUtils.replaceOnce(str, "#message", if (urlEncode) encode(message) else message)
    return str
  }

  /**
   * Used also for mocking [HttpGet] and [HttpPost].
   *
   * @param url
   * @return
   */
  protected fun createHttpMethod(url: String?, phoneNumber: String?, message: String): HttpRequestBase {
    if (config.httpMethodType === SmsSenderConfig.HttpMethodType.GET) {
      return try {
        val uriBuilder = URIBuilder(url)
        if (MapUtils.isNotEmpty(config.httpParams)) {
          // Now build the query params list from the configured httpParams:
          val params = arrayOfNulls<NameValuePair>(
            config.httpParams!!.size
          )
          for ((key, value1) in config.httpParams!!) {
            val value = replaceVariables(value1, phoneNumber, message, true)
            uriBuilder.setParameter(key, value)
          }
        }
        HttpGet(uriBuilder.build())
      } catch (ex: URISyntaxException) {
        log.error("Configuration error, can't build url: " + ex.message, ex)
        throw RuntimeException(ex)
      }
    }
    // HTTP POST
    val post = HttpPost(url)
    if (MapUtils.isNotEmpty(config.httpParams)) {
      // Now add all post params from the configured httpParams:
      val formparams: MutableList<NameValuePair> = ArrayList()
      for ((key, value1) in config.httpParams!!) {
        val value = replaceVariables(value1, phoneNumber, message, false)
        formparams.add(BasicNameValuePair(key, value))
        val entity = UrlEncodedFormEntity(formparams, Consts.UTF_8)
        post.entity = entity
      }
    }
    return post
  }

  protected fun createHttpClient(): CloseableHttpClient {
    return HttpClients.createDefault()
  }

  fun setConfig(config: SmsSenderConfig): SmsSender {
    this.config = config
    return this
  }

  companion object {
    /**
     * Uses UTF-8
     *
     * @param str
     * @see URLEncoder.encode
     */
    fun encode(str: String?): String {
      return if (str == null) {
        ""
      } else try {
        URLEncoder.encode(str, "UTF-8")
      } catch (ex: UnsupportedEncodingException) {
        log.info("Can't URL-encode '" + str + "': " + ex.message)
        ""
      }
    }
  }
}
