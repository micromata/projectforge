/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KotlinLogging
import org.apache.commons.codec.binary.Base64
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessChecker
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.menu.builder.getReactDynamicPageUrl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import jakarta.annotation.PostConstruct


private val log = KotlinLogging.logger {}

/**
 *
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
@Service
class SipgateClient {
  @Autowired
  internal lateinit var sipgateConfiguration: SipgateConfiguration

  @Autowired
  private lateinit var accessChecker: AccessChecker

  @Autowired
  private lateinit var menuCreator: MenuCreator

  private var debugConsoleOutForTesting = false

  private val timeoutMillis = 30000
  private val timeoutMillisLong = 30000L

  private var httpClient: HttpClient = HttpClient.create()
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
    .responseTimeout(Duration.ofMillis(timeoutMillisLong))
    .doOnConnected { conn ->
      conn.addHandlerLast(ReadTimeoutHandler(timeoutMillisLong, TimeUnit.MILLISECONDS))
        .addHandlerLast(WriteTimeoutHandler(timeoutMillisLong, TimeUnit.MILLISECONDS))
    }

  internal lateinit var webClient: WebClient

  class HttpException(val httpStatus: HttpStatus, message: String) : RuntimeException(message)

  @PostConstruct
  private fun postConstruct() {
    postConstruct(true)
  }

  internal fun postConstruct(useMenuCreator: Boolean) {
    val baseUri = sipgateConfiguration.baseUri
    val token = sipgateConfiguration.token
    val tokenId = sipgateConfiguration.tokenId
    val base64 = base64Credentials(tokenId = tokenId, token = token)

    val size = 16 * 1024 * 1024 // 16MB
    val strategies = ExchangeStrategies.builder()
      .codecs { codecs: ClientCodecConfigurer ->
        codecs.defaultCodecs().maxInMemorySize(size)
      }
      .build()

    webClient = WebClient.builder()
      .exchangeStrategies(strategies)
      .filters { exchangeFilterFunctions ->
        exchangeFilterFunctions.add(logRequest())
        exchangeFilterFunctions.add(logResponse())
      }
      .baseUrl(baseUri)
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .defaultHeader("Authorization", "Basic $base64")
      .defaultUriVariables(Collections.singletonMap("url", "baseUri"))
      .build()

    if (useMenuCreator && sipgateConfiguration.isConfigured()) {
      menuCreator
        .register(
          MenuItemDefId.ADMINISTRATION,
          MenuItemDef(id = "Sipgate", i18nKey = "sipgate.title", url = getReactDynamicPageUrl("sipgate"),
            checkAccess = { accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.ADMIN_GROUP) })
        )
    }
  }

  /**
   * @throws HttpException
   */
  internal fun <T> execute(
    headersSpec: WebClient.RequestHeadersSpec<*>,
    expectedReturnClass: Class<T>,
    successStatus: HttpStatus = HttpStatus.OK
  ): T {
    val mono = headersSpec
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .accept(MediaType.APPLICATION_JSON)
      .acceptCharset(StandardCharsets.UTF_8)
      .ifNoneMatch("*")
      .ifModifiedSince(ZonedDateTime.now())
      //.retrieve()
      //.bodyToMono(String::class.java)
      //.block()
      .exchangeToMono { response ->
        if (response.statusCode() == successStatus) {
          if (debugConsoleOutForTesting) {
            println("Success")
          }
          return@exchangeToMono response.bodyToMono(expectedReturnClass) as Mono<T>
        } else {
          log.error { "Error while calling $headersSpec." }
          if (debugConsoleOutForTesting) {
            println("Error: ${response.statusCode()}")
          }
          throw HttpException(response.statusCode(), "Error response: ${response.statusCode()}")
        }
      }
      .block()
    return mono
  }

  private fun logRequest(): ExchangeFilterFunction {
    return ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
      if (log.isDebugEnabled || debugConsoleOutForTesting) {
        val sb = StringBuilder("Request: \n")
        // append clientRequest method and url
        clientRequest
          .headers()
          .forEach { name: String?, values: List<String?> ->
            sb.append("header '$name': ${values.joinToString()}")
          }
        log.debug { sb.toString() }
        if (debugConsoleOutForTesting) {
          println(sb.toString())
        }
      }
      return@ofRequestProcessor Mono.just(clientRequest)
    }
  }

  private fun logResponse(): ExchangeFilterFunction {
    return ExchangeFilterFunction.ofResponseProcessor { clientRequest ->
      if (log.isDebugEnabled || debugConsoleOutForTesting) {
        val sb = StringBuilder("Response: \n")
        // append clientRequest method and url
        log.debug { sb.toString() }
        if (debugConsoleOutForTesting) {
          println(sb.toString())
        }
      }
      return@ofResponseProcessor Mono.just(clientRequest)
    }
  }

  companion object {
    internal fun base64Credentials(tokenId: String, token: String): String {
      return Base64.encodeBase64String("$tokenId:$token".toByteArray(Charsets.UTF_8))
    }
  }
}
