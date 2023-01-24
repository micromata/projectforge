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

package org.projectforge.rest.dvelop

import mu.KotlinLogging
import org.projectforge.business.dvelop.ListData
import org.projectforge.framework.json.JsonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

/**
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractService<T>(val path: String, val entityName: String) {
  class UpdateContext(var modified: Boolean = false)

  @Autowired
  internal lateinit var dvelopClient: DvelopClient

  internal var debugConsoleOutForTesting = false

  protected lateinit var webClient: WebClient

  @PostConstruct
  internal fun postConstruct() {
    webClient = dvelopClient.webClient
  }

  abstract fun fromJson(json: String): ListData<T>? // JsonUtils.fromJson(response, TradingPartnerListData::class.java, false)

  fun getList(start: Int = 0, count: Int = 50, maxNumberOfPages: Int = 100): List<T> {
    // Parameters: count=<pagesize>, continue=true, start=<page>
    val result = mutableListOf<T>()
    var pageCounter = 0
    var itemCounter = start
    while (pageCounter++ < maxNumberOfPages) {
      val uriSpec = webClient.get()
      val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path(path)
          .queryParam("start", itemCounter)
          .queryParam("count", count)
          .queryParam("continue", true)
          .build()
      }
      val response = dvelopClient.execute(headersSpec, String::class.java)
      if (debugConsoleOutForTesting) {
        println("response: $response")
      }
      val data = fromJson(response)
      val entities = data?.data ?: break
      val totalCount = data.totalCount ?: 0
      result.addAll(entities)
      itemCounter += count
      if (itemCounter > totalCount) {
        break
      }
    }
    log.info { "Got ${result.size} entries of $entityName from D.velop." }
    return result
  }

  // DELETE: {{baseUri}}/alphaflow-tradingpartner/tradingpartnerservice/tradingpartners/{{tradingPartnerID}}
  fun delete(id: String?, entity: T): Boolean {
    val json = JsonUtils.toJson(entity, true)
    if (debugConsoleOutForTesting) {
      println("Trying to delete $entityName #$id: $json")
    }
    if (id.isNullOrBlank()) {
      log.error { "Can't delete $entityName #$id: $json" }
      return false
    }
    log.info("Trying to delete $entityName #$id: $json")
    val uriSpec = webClient.delete()
    val headersSpec = uriSpec.uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("$path/{id}")
        .build(id)
    }

    val response = dvelopClient.execute(headersSpec, String::class.java)
    log.info { response }
    return true
  }

  fun create(entity: T): Boolean {
    val json = JsonUtils.toJson(entity, true)
    try {
      val uriSpec = webClient.post()
      if (debugConsoleOutForTesting) {
        println("create body: $json")
      }
      log.info { "Trying to create $entityName in D.velop: $json" }
      val bodySpec = uriSpec.uri(path)
        .body(
          BodyInserters.fromValue(json)
        )
      val response = dvelopClient.execute(bodySpec, String::class.java)
      if (debugConsoleOutForTesting) {
        println("response: $response")
      }
      return true
    } catch (ex: Exception) {
      log.error("Error while creating $entityName in D.velop: ${ex.message}: $json")
      return false
    }
  }

  /**
   * This method should determine which fields of which object should be prioritized: remote or local value of the field.
   * @return The remoteState object with updated fields or null, if nothing is to update (all fields of remoteState are up-to-date).
   */
  abstract fun buildUpdateEntity(localState: T, remoteState: T): T?

  /**
   * Updates the remoteState only, if there is any field to update. Otherwise, nothing will be modified.
   * @return true if the remoteState was updated or false if the remoteState of each field is up-to-date.
   * @see buildUpdateEntity
   */
  fun update(localState: T, remoteState: T): Boolean {
    val updateEntity = buildUpdateEntity(localState, remoteState) ?: return false
    val json = JsonUtils.toJson(updateEntity, true)
    try {
      val uriSpec = webClient.patch()
      if (debugConsoleOutForTesting) {
        println("update body: $json")
      }
      log.info { "Trying to update $entityName in D.velop: $json" }
      val bodySpec = uriSpec.uri(path)
        .body(
          BodyInserters.fromValue(json)
        )
      val response = dvelopClient.execute(bodySpec, String::class.java)
      if (debugConsoleOutForTesting) {
        println("response: $response")
      }
      return true
    } catch (ex: Exception) {
      log.error("Error while updating $entityName in D.velop: ${ex.message}: $json")
      return false
    }
  }

  companion object {
    internal fun getPrioritizedString(priority1: String?, priority2: String?, updateContext: UpdateContext): String? {
      val p1 = if (priority1.isNullOrBlank()) null else priority1
      val p2 = if (priority2.isNullOrBlank()) null else priority2
      if (p1 == p2) {
        return priority1
      }
      updateContext.modified = true
      return if (p1 == null) {
        priority2
      } else {
        priority1
      }
    }

    /**
     * For strings, you should (but mustn't) use getPrioritizedString instead.
     */
    internal fun <T> getPrioritizedValue(priority1: T?, priority2: T?, updateContext: UpdateContext): T? {
      if (priority1 is String? && priority2 is String?) {
        // Calling this function with Strings should use getPrioritizedString instead.
        @Suppress("UNCHECKED_CAST")
        return getPrioritizedString(priority1 as String?, priority2 as String?, updateContext) as T?
      }
      if (priority1 == priority2) {
        return priority1
      }
      updateContext.modified = true
      return priority1 ?: priority2
    }
  }
}
