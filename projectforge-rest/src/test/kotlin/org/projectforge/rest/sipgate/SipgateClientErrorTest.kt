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

package org.projectforge.rest.sipgate

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * A failed read of Sipgate must not look like an empty list (the contact sync would delete its records and
 * create every contact again), and an error answer keeps the reason Sipgate gives.
 */
class SipgateClientErrorTest {
  private fun answering(status: HttpStatus, body: String): Pair<WebClient, SipgateClient> {
    val exchange = ExchangeFunction {
      Mono.just(
        ClientResponse.create(status)
          .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
          .body(body).build()
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchange).baseUrl("http://localhost").build()
    val client = SipgateClient()
    client.webClient = webClient
    return Pair(webClient, client)
  }

  private fun read(webClient: WebClient, client: SipgateClient, throwOnError: Boolean) =
    SipgateService.getList(
      webClient, client, path = "/contacts", entityName = "Contact",
      listDataClass = ContactListData::class.java, throwOnError = throwOnError,
    )

  @Test
  fun `failed read throws with the reason of Sipgate if requested`() {
    val (webClient, client) = answering(HttpStatus.BAD_REQUEST, """{"message":"limit too large"}""")
    val ex = assertThrows<SipgateClient.HttpException> { read(webClient, client, true) }
    assertTrue(ex.message!!.contains("limit too large"), ex.message)
  }

  @Test
  fun `failed read answers an empty list by default`() {
    val (webClient, client) = answering(HttpStatus.BAD_REQUEST, "{}")
    assertEquals(0, read(webClient, client, false).size)
  }
}
