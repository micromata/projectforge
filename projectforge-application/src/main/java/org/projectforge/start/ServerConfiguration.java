package org.projectforge.start;

import org.apache.catalina.connector.Connector;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration implements EmbeddedServletContainerCustomizer
{

  static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ServerConfiguration.class);

  @Value("${projectforge.servletContextPath}")
  private String servletContextPath;

  @Value("${tomcat.ajp.port}")
  private int ajpPort;

  @Value("${tomcat.ajp.enabled:false}")
  private boolean tomcatAjpEnabled;

  @Override
  public void customize(ConfigurableEmbeddedServletContainer container)
  {
    if (StringUtils.isBlank(servletContextPath) == false) {
      container.setContextPath(servletContextPath);
    }

    if (container instanceof TomcatEmbeddedServletContainerFactory) {
      TomcatEmbeddedServletContainerFactory tomcatContainer = (TomcatEmbeddedServletContainerFactory) container;
      if (tomcatAjpEnabled) {
        Connector ajpConnector = new Connector("AJP/1.3");
        ajpConnector.setProtocol("AJP/1.3");
        ajpConnector.setPort(ajpPort);
        ajpConnector.setSecure(false);
        ajpConnector.setAllowTrace(false);
        ajpConnector.setScheme("http");
        tomcatContainer.getAdditionalTomcatConnectors().add(ajpConnector);
      }
    }

  }
}
