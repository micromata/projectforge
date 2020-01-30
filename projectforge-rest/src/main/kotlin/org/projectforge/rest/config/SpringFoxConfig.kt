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

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.paths.AbstractPathProvider
import springfox.documentation.spring.web.paths.Paths
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


@Configuration
@EnableSwagger2
open class SpringFoxConfig {
    @Bean
    open fun api(): Docket? {
        return Docket(DocumentationType.SWAGGER_2)
                //.pathProvider(BasePathAwareRelativePathProvider("/secure/swagger")) // Doesn't work.
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
    }
}

internal class BasePathAwareRelativePathProvider(private val basePath: String) : AbstractPathProvider() {
    override fun applicationPath(): String {
        return basePath
    }

    override fun getDocumentationPath(): String {
        return basePath
    }

    override fun getOperationPath(operationPath: String): String {
        val uriComponentsBuilder: UriComponentsBuilder = UriComponentsBuilder.fromPath("/")
        return Paths.removeAdjacentForwardSlashes(
                uriComponentsBuilder.path(operationPath.replaceFirst(basePath.toRegex(), "")).build().toString())
    }

}
