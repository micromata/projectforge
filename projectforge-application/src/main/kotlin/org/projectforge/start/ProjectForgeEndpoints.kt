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

package org.projectforge.start

import mu.KotlinLogging
import org.apache.wicket.Page
import org.apache.wicket.markup.html.WebPage
import org.projectforge.SystemStatus
import org.projectforge.security.My2FARequestHandler
import org.projectforge.web.admin.AdminPage
import org.projectforge.web.admin.IProjectForgeEndpoints
import org.projectforge.web.registry.WebRegistry
import org.projectforge.web.wicket.AbstractUnsecureBasePage
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Modifier
import jakarta.annotation.PostConstruct

private val log = KotlinLogging.logger {}

@Service
class ProjectForgeEndpoints : IProjectForgeEndpoints {
  private lateinit var restEndPointsMap: Map<RequestMappingInfo, HandlerMethod>
  private lateinit var wicketPagesMap: Map<String, Class<out WebPage>>
  private var initialized = false

  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var my2FARequestHandler: My2FARequestHandler

  @PostConstruct
  private fun postConstruct() {
    AdminPage.set(this)
  }

  private fun ensureEndpoints() {
    synchronized(this) {
      if (initialized) {
        return
      }
      val newWicketPagesMap = WebRegistry.getInstance().getMountPages()
      val requestMappingHandlerMapping = applicationContext.getBean(RequestMappingHandlerMapping::class.java)
      restEndPointsMap = requestMappingHandlerMapping.handlerMethods
      if (log.isDebugEnabled) {
        restEndPointsMap.forEach { (key: RequestMappingInfo?, value: HandlerMethod?) ->
          log.debug { "key=$key, value=$value" }
        }
      }

      val reflections = Reflections("org.projectforge")
      val wicketPages =
        reflections.get(Scanners.SubTypes.of(AbstractUnsecureBasePage::class.java).asClass<Class<out Page>>())
      wicketPages.forEach { pageClass ->
        if (!newWicketPagesMap.containsValue(pageClass) && !Modifier.isAbstract(pageClass.getModifiers())) {
          pageClass as Class<out WebPage>
          newWicketPagesMap[WebRegistry.getInstance().getMountPoint(pageClass)] = pageClass
        }
      }
      wicketPagesMap = newWicketPagesMap
      println(wicketPages.joinToString { it.name })

      initialized = true
    }
  }

  override fun getInfo(): String {
    ensureEndpoints()
    val out = StringWriter()
    val pw = PrintWriter(out)
    pw.println("2FA configuration")
    pw.println("-----------------")
    pw.println("")
    pw.println("1. effective configuration")
    pw.println("--------------------------")
    pw.println(my2FARequestHandler.printConfiguration())
    pw.println("")
    pw.println("2. short cuts")
    pw.println("------------")
    pw.println(my2FARequestHandler.printResolvedShortCuts())
    pw.println("3. endpoints")
    pw.println("------------")
    val endpoints: MutableList<String> = ArrayList()
    restEndPointsMap.forEach { (info: RequestMappingInfo, method: HandlerMethod?) ->
      if (!info.directPaths.isEmpty()) {
        endpoints.add(info.directPaths.iterator().next())
      }
    }
    wicketPagesMap.forEach { mountPoint, clazz ->
      endpoints.add(WebRegistry.getInstance().getMountPoint(clazz))
    }
    pw.println(my2FARequestHandler.printAllEndPoints(endpoints))
    return out.toString()
  }

  @EventListener(ApplicationReadyEvent::class)
  fun onApplicationReady() {
    if (SystemStatus.isDevelopmentMode()) {
      log.info(info)
    }
  }
}
