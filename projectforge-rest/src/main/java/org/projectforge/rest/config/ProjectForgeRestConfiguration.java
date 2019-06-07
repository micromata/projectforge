/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.config;

import org.glassfish.jersey.servlet.ServletContainer;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.web.rest.RestUserFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectForgeRestConfiguration {
  /**
   * Replaced by {@link Rest#URL}
   */
  @Deprecated
  public static final String REST_WEB_APP_URL = Rest.URL + "/";
  /**
   * Replaced by {@link Rest#PUBLIC_URL}
   */
  @Deprecated
  public static final String REST_WEB_APP_PUBLIC_URL = Rest.PUBLIC_URL + "/";

  @Bean
  public ServletRegistrationBean publicJersey() {
    ServletRegistrationBean publicJersey
            = new ServletRegistrationBean(new ServletContainer(new RestPublicConfiguration()));
    publicJersey.addUrlMappings("/" + RestPaths.PUBLIC_REST + "/*");
    publicJersey.setName("RestPublic");
    publicJersey.setLoadOnStartup(0);
    return publicJersey;
  }

  @Bean
  public ServletRegistrationBean privateJersey() {
    ServletRegistrationBean privateJersey
            = new ServletRegistrationBean(new ServletContainer(new RestPrivateConfiguration()));
    privateJersey.addUrlMappings("/" + RestPaths.REST + "/*");
    privateJersey.setName("RestPrivate");
    privateJersey.setLoadOnStartup(0);
    return privateJersey;
  }

  @Bean
  public FilterRegistrationBean<RestUserFilter> webAppJersey() {
    FilterRegistrationBean<RestUserFilter> registrationBean
            = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RestUserFilter());
    registrationBean.addUrlPatterns(Rest.URL + "*");
    return registrationBean;
  }
}
