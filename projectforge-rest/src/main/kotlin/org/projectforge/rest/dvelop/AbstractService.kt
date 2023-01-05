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
 * Handles the login to the dvelop server (if configured and in use).
 *
 * Fragen
 * * Datev-Konto als Entität
 *
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

  abstract fun buildUpdateEntity(localState: T, remoteState: T): T?

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

  protected fun getPrioritizedString(priority1: String?, priority2: String?, updateContext: UpdateContext): String? {
    if (priority1 == priority2 || priority1.isNullOrEmpty() && priority2.isNullOrEmpty()) {
      return priority1
    }
    return if (priority1.isNullOrBlank()) {
      if (!priority2.isNullOrBlank()) {
        updateContext.modified = true
      }
      priority2
    } else {
      updateContext.modified = true
      priority1
    }
  }

  protected fun <T> getPrioritizedValue(priority1: T?, priority2: T?, updateContext: UpdateContext): T? {
    if (priority1 != null && priority1 is String || priority2 != null && priority2 is String) {
      return getPrioritizedString(priority1 as String?, priority2 as String?, updateContext) as T?
    }
    if (priority1 == priority2) {
      return priority1
    }
    updateContext.modified = true
    return priority1 ?: priority2
  }
}
