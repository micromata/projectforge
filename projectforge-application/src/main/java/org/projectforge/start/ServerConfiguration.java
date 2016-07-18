package org.projectforge.start;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("file:projectforge.properties")
public class ServerConfiguration implements EmbeddedServletContainerCustomizer
{

  static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ServerConfiguration.class);

  @Value("${projectforge.servletContextPath}")
  private String servletContextPath;

  @Override
  public void customize(ConfigurableEmbeddedServletContainer container)
  {
    if (StringUtils.isBlank(servletContextPath) == false) {
      container.setContextPath(servletContextPath);
    }

    if (container instanceof JettyEmbeddedServletContainerFactory) {
      //customizeJetty((JettyEmbeddedServletContainerFactory) container);
    }
  }

  //  private void customizeJetty(JettyEmbeddedServletContainerFactory factory)
  //  {
  //    factory.addServerCustomizers(new JettyServerCustomizer()
  //    {
  //
  //      @Override
  //      public void customize(Server server)
  //      {
  //        SslContextFactory sslContextFactory = new SslContextFactory();
  //        sslContextFactory.setKeyStorePassword("jetty6");
  //        try {
  //          sslContextFactory.setKeyStorePath(ResourceUtils.getFile(
  //              "classpath:jetty-ssl.keystore").getAbsolutePath());
  //        } catch (FileNotFoundException ex) {
  //          throw new IllegalStateException("Could not load keystore", ex);
  //        }
  //        SslSocketConnector sslConnector = new SslSocketConnector(
  //            sslContextFactory);
  //        sslConnector.setPort(9993);
  //        sslConnector.setMaxIdleTime(60000);
  //        server.addConnector(sslConnector);
  //      }
  //    });
  //  }

  //INFOs:
  //http://stackoverflow.com/questions/24122123/spring-boot-jetty-ssl-port
}
