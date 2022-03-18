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

import mu.KotlinLogging
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.menu.builder.MenuItemDefId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import javax.annotation.PostConstruct

private val log = KotlinLogging.logger {}

@Configuration
@EnableSwagger2
open class SpringFoxConfig {
  @Autowired
  private lateinit var menuCreator: MenuCreator

  @Autowired
  private lateinit var environment: Environment

  @PostConstruct
  private fun init() {
    swaggerEnabled = environment.getProperty(CONFIG_PROPERTY) == "true"
    if (swaggerEnabled) {
      EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL)
        .log("Swagger (API documentation and test center) enabled: ${SwaggerUIFilter.SWAGGER_ROOT}")
        .logEnd()
      menuCreator.register(
        MenuItemDefId.MISC,
        MenuItemDef("swagger", "menu.misc.swagger", "${SwaggerUIFilter.SWAGGER_ROOT_NON_TRAILING_SLASH}swagger-ui/")
      )
    } else {
      log.info { "Swagger not enabled ($CONFIG_PROPERTY=false)." }
    }
  }

  @Bean
  open fun api(): Docket {
    return Docket(DocumentationType.SWAGGER_2)
      .select()
      .apis(RequestHandlerSelectors.any())
      .paths(PathSelectors.any())
      .build()
  }

  companion object {
    var swaggerEnabled = false
      private set

    private const val CONFIG_PROPERTY = "springfox.documentation.enabled"
  }
}
