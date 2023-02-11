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
import org.projectforge.framework.json.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
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

  abstract fun setId(obj: T, id: String)

  open fun getList(offset: Int = 0, limit: Int = 5000, maxNumberOfPages: Int = 100): List<T> {
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
      val data = fromJson(response)
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
  }

  open fun create(entity: T): Boolean {
    val json = JsonUtils.toJson(entity, true)
    try {
      val uriSpec = webClient.post()
      if (debugConsoleOutForTesting) {
        println("create body: $json")
      }
      log.info { "${getLogInfo(entity)}: Trying to create $entityName in Sipgate: $json" }
      val bodySpec = uriSpec.uri(path)
        .body(
          BodyInserters.fromValue(json)
        )
      val response = sipgateClient.execute(bodySpec, String::class.java, HttpStatus.CREATED)
      if (debugConsoleOutForTesting) {
        println("response: $response")
      }
      return true
    } catch (ex: Exception) {
      log.error("${getLogInfo(entity)}: Error while creating $entityName in Sipgate: ${ex.message}: $json")
      return false
    }
  }

  open fun delete(id: String?, entity: T): Boolean {
    val json = JsonUtils.toJson(entity, true)
    if (debugConsoleOutForTesting) {
      println("Trying to delete $entityName #$id: $json")
    }
    if (id.isNullOrBlank()) {
      log.error { "${getLogInfo(entity)}: Can't delete $entityName #$id: $json" }
      return false
    }
    log.info("${getLogInfo(entity)}: Trying to delete $entityName #$id: $json")
    val uriSpec = webClient.delete()
    val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("$path/{id}")
        .build(id)
    }

    val response = sipgateClient.execute(headersSpec, String::class.java)
    log.info { "${getLogInfo(entity)}: response=[$response]" }
    return true
  }

  /**
   * Updates the remoteState only, if there is any field to update. Otherwise, nothing will be modified.
   * @return true if the remoteState was updated or false if the remoteState of each field is up-to-date.
   * @see buildUpdateEntity
   */
  open fun update(id: String, entity: T): Boolean {
    setId(entity, id)
    val json = JsonUtils.toJson(entity, true)
    try {
      val uriSpec = webClient.put()
      val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("$path/{id}")
          .build(id)
      }
      if (debugConsoleOutForTesting) {
        println("update body: $json")
      }
      log.info { "${getLogInfo(entity)}: Trying to update $entityName #$id in Sipgate: $json" }
      val bodySpec = headersSpec.body(
          BodyInserters.fromValue(json)
        )
      val response = sipgateClient.execute(bodySpec, String::class.java, HttpStatus.NO_CONTENT)
      if (debugConsoleOutForTesting) {
        println("response: $response")
      }
      return true
    } catch (ex: Exception) {
      log.error("${getLogInfo(entity)}: Error while updating $entityName in Sipgate: ${ex.message}: $json")
      return false
    }
  }

  abstract fun getLogInfo(entity: T): String
}
