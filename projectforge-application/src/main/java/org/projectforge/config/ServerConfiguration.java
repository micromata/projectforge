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

package org.projectforge.config;

import org.apache.catalina.connector.Connector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration
{
  static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServerConfiguration.class);

  @Value("${projectforge.servletContextPath}")
  private String servletContextPath;

  @Value("${tomcat.ajp.port:8009}")
  private int ajpPort;

  @Value("${tomcat.ajp.enabled:false}")
  private boolean tomcatAjpEnabled;

  @Bean
  public ServletWebServerFactory servletContainer()
  {
    TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
    if (StringUtils.isNotBlank(servletContextPath)) {
      tomcat.setContextPath(servletContextPath);
    }
    if (tomcatAjpEnabled) {
      tomcat.addAdditionalTomcatConnectors(createAJPConnector());
    }
    return tomcat;
  }

  private Connector createAJPConnector()
  {
    final Connector ajpConnector = new Connector("AJP/1.3");
    ajpConnector.setPort(ajpPort);
    ajpConnector.setAttribute("address", "127.0.0.1");
    ajpConnector.setSecure(false);
    ajpConnector.setAllowTrace(false);
    ajpConnector.setScheme("http");
    return ajpConnector;
  }
}
