package org.projectforge.launcher.config;

import org.apache.commons.codec.Charsets;

import de.micromata.genome.logging.config.LsLoggingLocalSettingsConfigModel;
import de.micromata.genome.util.runtime.config.AbstractCompositLocalSettingsConfigModel;
import de.micromata.genome.util.runtime.config.AbstractTextConfigFileConfigModel;
import de.micromata.genome.util.runtime.config.HibernateSchemaConfigModel;
import de.micromata.genome.util.runtime.config.JdbcLocalSettingsConfigModel;
import de.micromata.genome.util.runtime.config.JndiLocalSettingsConfigModel;
import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;
import de.micromata.mgc.application.webserver.config.JettyConfigModel;
import de.micromata.mgc.email.MailReceiverLocalSettingsConfigModel;
import de.micromata.mgc.javafx.launcher.gui.generic.LauncherLocalSettingsConfigModel;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class PfLocalSettingsConfigModel extends AbstractCompositLocalSettingsConfigModel
{

  PfBasicLocalSettingsConfigModel pfBasicConfig = new PfBasicLocalSettingsConfigModel();

  @SuppressWarnings("unused")
  private LauncherLocalSettingsConfigModel launcherConfig = new LauncherLocalSettingsConfigModel();
  @SuppressWarnings("unused")
  private JettyConfigModel jettyConfigModel = new JettyConfigModel();
  @SuppressWarnings("unused")
  private MailSessionLocalSettingsConfigModel emailConfig = new MailSessionLocalSettingsConfigModel("pfmailsession");

  @SuppressWarnings("unused")
  private HibernateSchemaConfigModel hibernateSchemaConfig = new HibernateSchemaConfigModel();
  @SuppressWarnings("unused")
  private AbstractTextConfigFileConfigModel log4jConfig = new AbstractTextConfigFileConfigModel("Log4J",
      "log4j.properties", Charsets.ISO_8859_1);
  @SuppressWarnings("unused")
  private JdbcLocalSettingsConfigModel jdbcConfigModel = new JdbcLocalSettingsConfigModel("projectForgeDs",
      "Standard JDBC for Genome");
  @SuppressWarnings("unused")
  private MailReceiverLocalSettingsConfigModel mailReceiverConfigModel = new MailReceiverLocalSettingsConfigModel();
  @SuppressWarnings("unused")
  private LsLoggingLocalSettingsConfigModel loggingConfigModel = new LsLoggingLocalSettingsConfigModel();

  public PfLocalSettingsConfigModel()
  {
    JndiLocalSettingsConfigModel dsWeb = new JndiLocalSettingsConfigModel("dsWeb",
        JndiLocalSettingsConfigModel.DataType.DataSource, "java:comp/env/projectForge/jdbc/dsWeb");
    dsWeb.setSource("projectForgeDs");
    dsWeb.setSectionComment("For Web Transaction");
    jdbcConfigModel.getAssociatedJndi().add(dsWeb);
  }
}
