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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractSipgateService<T>(val path: String, val entityName: String) {

  @Autowired
  internal lateinit var sipgateClient: SipgateClient

  internal var debugConsoleOutForTesting = false

  protected lateinit var webClient: WebClient

  @PostConstruct
  internal fun postConstruct() {
    webClient = sipgateClient.webClient
  }

  abstract fun fromJson(json: String): ListData<T>? // JsonUtils.fromJson(response, TradingPartnerListData::class.java, false)

  fun getList(): List<T> {
    // Parameters: count=<pagesize>, continue=true, start=<page>
    val uriSpec = webClient.get()
    val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
      uriBuilder.path(path).build()
    }
    val response = sipgateClient.execute(headersSpec, String::class.java)
    if (debugConsoleOutForTesting) {
      println("response: $response")
    }
    val result = mutableListOf<T>()
    fromJson(response)?.items?.let { result.addAll(it) }
    log.info { "Got ${result.size} entries of $entityName from Sipgate." }
    return result
  }
}
