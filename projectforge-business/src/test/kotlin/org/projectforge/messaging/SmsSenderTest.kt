/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.Parameter
import org.mockserver.model.ParameterBody
import org.projectforge.messaging.SmsSender.HttpResponseCode
import org.projectforge.sms.SmsSenderConfig

class SmsSenderTest {
  private var counter = 0

  @Test
  @Throws(Exception::class)
  fun testSmsService() {
    testCalls(200, "OK, perfect!")
    testCalls(200, "Authentication failed", expectedResponseCode = HttpResponseCode.UNKNOWN_ERROR)
    testCalls(400, "OK", expectedResponseCode = HttpResponseCode.UNKNOWN_ERROR)
  }

  private fun testCalls(
    httpStatus: Int,
    httpResponse: String,
    expectedResponseCode: HttpResponseCode = HttpResponseCode.SUCCESS,
    path: String? = null
  ) {
    val usePath = path ?: "$PATH${++counter}"
    testCall(false, httpStatus, httpResponse, expectedResponseCode, usePath) // GET-call
    testCall(true, httpStatus, httpResponse, expectedResponseCode, usePath)  // POST-call
  }

  private fun testCall(
    post: Boolean,
    httpStatus: Int,
    httpResponse: String,
    expectedResponseCode: HttpResponseCode = HttpResponseCode.SUCCESS,
    path: String = "$PATH${++counter}"
  ) {
    createClientServer(post, httpStatus, httpResponse, path = path)
    val sender = createSender(post, path)
    val responseCode = sender.send(TO, MESSAGE)
    Assertions.assertEquals(expectedResponseCode, responseCode)

  }

  private fun createSender(
    post: Boolean,
    path: String,
    user: String = USER,
    password: String = PASSWORD,
  ): SmsSender {
    val params = createParams("user", user, "password", password, "message", "#message", "to", "#number")
    val config = SmsSenderConfig()
    config.setHttpMethodType(if (post) "post" else "get").url = "http://127.0.0.1:$PORT/$path"
    config.httpParams = params
    config.smsReturnPatternSuccess = "^OK.*"
    config.smsReturnPatternError = "ERROR"
    config.smsReturnPatternMessageError = "MESSAGE ERROR"
    config.smsReturnPatternMessageToLargeError = ".*LARGE.*"
    config.smsReturnPatternNumberError = "NUMBER FORMAT ERROR"
    return SmsSender(config)
  }

  private fun assertContains(str: String, part: String) {
    Assertions.assertTrue(str.contains(part), "String '$str' must contain '$part'.")
  }

  private fun createParams(vararg params: String): Map<String, String> {
    Assertions.assertTrue(params.size % 2 == 0) // even number.
    val map: MutableMap<String, String> = HashMap()
    var i = 0
    while (i < params.size) {
      map[params[i]] = params[i + 1]
      i += 2
    }
    return map
  }

  private fun createClientServer(
    post: Boolean,
    returnHttpCode: Int,
    httpResponse: String,
    message: String = MESSAGE,
    to: String = TO,
    user: String = USER,
    password: String = PASSWORD,
    path: String,
  ): MockServerClient {
    val method = if (post) "POST" else "GET"
    val sc = MockServerClient("127.0.0.1", PORT)
    val request = HttpRequest.request()
      .withMethod(method)
      .withPath("/$path")
    if (post) {
      preparePostRequest(request, message = message, to = to, user = user, password = password)
    } else {
      prepareGetRequest(request, message = message, to = to, user = user, password = password)
    }
    sc.`when`(request)
      .respond(
        HttpResponse.response()
          .withStatusCode(returnHttpCode)
          .withBody(httpResponse)
      )
    return sc
  }

  private fun prepareGetRequest(request: HttpRequest, message: String, to: String, user: String, password: String) {
    request
      .withQueryStringParameter("user", user)
      .withQueryStringParameter("password", password)
      .withQueryStringParameter("message", message)
      .withQueryStringParameter("to", to)
  }

  private fun preparePostRequest(request: HttpRequest, message: String, to: String, user: String, password: String) {
    request.withBody(
      ParameterBody.params(
        Parameter.param("user", user),
        Parameter.param("password", password),
        Parameter.param("message", message),
        Parameter.param("to", to)
      )
    )
  }


  companion object {
    private const val PORT = 65123
    private const val TO = "0123456789"
    private const val MESSAGE = "Hello_world"
    private const val USER = "smsUser"
    private const val PASSWORD = "smsPassword"
    private const val PATH = "send"
    private lateinit var mockServer: ClientAndServer

    @BeforeClass
    @JvmStatic
    fun startServer() {
      mockServer = ClientAndServer.startClientAndServer(PORT)
    }

    @AfterClass
    @JvmStatic
    fun stopServer() {
      mockServer.stop()
    }
  }
}
