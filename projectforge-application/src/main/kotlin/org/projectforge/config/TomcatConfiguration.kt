/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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
package org.projectforge.config

import mu.KotlinLogging
import org.apache.catalina.connector.Connector
import org.apache.commons.lang3.StringUtils
import org.apache.coyote.ajp.AbstractAjpProtocol
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private val log = KotlinLogging.logger {}

@Configuration
open class TomcatConfiguration {
  @Value("\${projectforge.servletContextPath}")
  private var servletContextPath: String? = null

  @Value("\${tomcat.ajp.address:127.0.0.1}")
  private var ajpAddress = "127.0.0.1"

  @Value("\${tomcat.ajp.port:8009}")
  private var ajpPort = 8009

  @Value("\${tomcat.ajp.enabled:false}")
  private var ajpEnabled = false

  @Value("\${tomcat.ajp.secure:false}")
  private var ajpSecure = false

  @Value("\${tomcat.ajp.secretRequired:false}")
  private var ajpSecretRequired = false

  @Value("\${tomcat.ajp.scheme:http}")
  private var ajpScheme = "http"

  @Value("\${tomcat.ajp.allowTrace:false}")
  private var allowTrace = false

  @Bean
  open fun servletContainer(): ServletWebServerFactory {
    val tomcat = TomcatServletWebServerFactory()
    if (StringUtils.isNotBlank(servletContextPath)) {
      tomcat.contextPath = servletContextPath
    }
    if (ajpEnabled) {
      tomcat.addAdditionalTomcatConnectors(createAJPConnector())
    }
    return tomcat
  }

  private fun createAJPConnector(): Connector {
    val ajpConnector = Connector("AJP/1.3")
    ajpConnector.port = ajpPort
    ajpConnector.setProperty("address", ajpAddress)
    ajpConnector.secure = ajpSecure
    ajpConnector.allowTrace = allowTrace
    ajpConnector.scheme = ajpScheme
    (ajpConnector.protocolHandler as AbstractAjpProtocol<*>).secretRequired = ajpSecretRequired
    log.info("Starting AJP connector: $ajpAddress:$ajpPort, secretRequired=$ajpSecretRequired, secure=$ajpSecure")
    return ajpConnector
  }
}
