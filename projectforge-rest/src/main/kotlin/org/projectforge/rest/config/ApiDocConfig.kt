/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.config

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import mu.KotlinLogging
import org.projectforge.ProjectForgeVersion
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.springdoc.core.GroupedOpenApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Admin
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.annotation.PostConstruct
import kotlin.system.exitProcess


private val log = KotlinLogging.logger {}

@Configuration
open class ApiDocConfig {
  @Autowired
  private lateinit var applicationContext: ApplicationContext

  @Autowired
  private lateinit var environment: Environment

  @Autowired
  private lateinit var menuCreator: MenuCreator

  @PostConstruct
  private fun init() {
    apiDocEnabled = environment.getProperty(CONFIG_SPRING_DOC_ENABLED) == "true"
    if (environment.getProperty(CONFIG_SWAGGER_UI_PATH)?.startsWith(ApiDocFilter.APIDOC_ROOT) != true ||
      environment.getProperty(CONFIG_API_DOC_PATH)?.startsWith(ApiDocFilter.APIDOC_ROOT) != true
    ) {
      // Paranoia check:
      log.error { "****** Config: $CONFIG_SWAGGER_UI_ENABLED=${environment.getProperty(CONFIG_SWAGGER_UI_PATH)}" }
      log.error { "****** Config: $CONFIG_API_DOC_PATH=${environment.getProperty(CONFIG_API_DOC_PATH)}" }
      log.error { "****** Expected root of ApiDocFilter: ${ApiDocFilter.APIDOC_ROOT}" }
      EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.VERY_IMPORTANT).log("Security shuddown:")
        .log("Misconfiguration of SpringDoc / Swagger (see above).").logEnd()
      val exitCode = SpringApplication.exit(applicationContext, ExitCodeGenerator { 0 })
      exitProcess(exitCode)
    }
    if (apiDocEnabled) {
      HTML_PAGE = environment.getProperty(CONFIG_SWAGGER_UI_PATH).removePrefix("/")
      EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL)
        .log("Spring (API documentation and test center) enabled: $HTML_PAGE")
        .logEnd()
      menuCreator.register(
        MenuItemDefId.MISC,
        MenuItemDef("apidoc", "menu.misc.apidoc", HTML_PAGE)
      )
    } else {
      log.info { "Swagger not enabled ($CONFIG_SPRING_DOC_ENABLED=false)." }
    }
  }

  @Bean
  open fun springProjectForgeOpenAPI(): OpenAPI {
    return OpenAPI()
      .info(
        Info().title("${ProjectForgeVersion.APP_ID} API")
          .version(ProjectForgeVersion.VERSION_STRING)
          .license(License().name("Dual licensed").url("http://www.projectforge.org"))
      )
      .externalDocs(
        ExternalDocumentation()
          .description("ProjectForge Documentation")
          .url("https://github.com/micromata/projectforge")
      )
  }
/*
  @Bean
  open fun publicApi(): GroupedOpenApi? {
    return GroupedOpenApi.builder()
      .group("projectforge-public")
      .pathsToMatch("/rsPublic/ **")
      .build()
  }

  @Bean
  open fun privateApi(): GroupedOpenApi? {
    return GroupedOpenApi.builder()
      .group("projectforge")
      .pathsToMatch("/rs/ **")
      //.addMethodFilter { method -> method.isAnnotationPresent(Admin::class.java) }
      .build()
  }
*/
  companion object {
    var apiDocEnabled = false
      private set

    private var HTML_PAGE = "<not initialized/configured>"

    private const val CONFIG_SPRING_DOC_ENABLED = "springdoc.api-docs.enabled"
    private const val CONFIG_SWAGGER_UI_ENABLED = "springdoc.swagger-ui.enabled"

    private const val CONFIG_API_DOC_PATH = "springdoc.api-docs.path"
    private const val CONFIG_SWAGGER_UI_PATH = "springdoc.swagger-ui.path"
  }
}
