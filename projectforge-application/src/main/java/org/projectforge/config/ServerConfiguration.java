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
