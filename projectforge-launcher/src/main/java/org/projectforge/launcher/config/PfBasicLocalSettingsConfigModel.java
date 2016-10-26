package org.projectforge.launcher.config;

import java.io.File;

import de.micromata.genome.util.runtime.config.ALocalSettingsPath;
import de.micromata.genome.util.runtime.config.AbstractLocalSettingsConfigModel;
import de.micromata.genome.util.runtime.config.LocalSettingsWriter;
import de.micromata.genome.util.validation.ValContext;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class PfBasicLocalSettingsConfigModel extends AbstractLocalSettingsConfigModel
{

  @ALocalSettingsPath(key = "spring.application.name", comment = "SPRING PROPERTIES", defaultValue = "projectforge-application")
  private String applicationName;

  @ALocalSettingsPath(key = "projectforge.base.dir", comment = "PROJECTFORGE PROPERTIES", defaultValue = "pfconfig")
  private String baseDir;

  @ALocalSettingsPath(key = "projectforge.domain", defaultValue = "https://projectforge.micromata.de")
  private String projectforgeDomain;

  @ALocalSettingsPath(key = "projectforge.wicket.developmentMode", defaultValue = "false")
  private String projectforgeWicketDevelopmentMode;

  @ALocalSettingsPath(key = "projectforge.testsystemMode", defaultValue = "false")
  private String projectforgeTestsystemMode;

  @ALocalSettingsPath(key = "projectforge.login.handlerClass",
      comment = "LoginDefaultHandler LdapMasterLoginHandler LdapSlaveLoginHandler",
      defaultValue = "LoginDefaultHandler")
  private String projectforgeLoginHandler;

  @ALocalSettingsPath(key = "hibernate.search.default.indexBase",
      defaultValue = "${projectforge.base.dir}/hibernateSearch")
  private String hibernateSearchDefaultIndexBase;

  @ALocalSettingsPath(key = "projectforge.resourcesDirectory",
      defaultValue = "${projectforge.base.dir}/resources")
  private String projectForgeResourceDirectory;

  @ALocalSettingsPath(key = "projectforge.fontsDirectory",
      defaultValue = "${projectforge.base.dir}/fonts")
  private String projectFontsDirectory;

  @ALocalSettingsPath(key = "projectforge.logoFile", defaultValue = "Micromata.png")
  private String projectForgeLogoFile;

  @ALocalSettingsPath(key = "projectforge.export.logoFile", defaultValue = "Micromata.png")
  private String projectForgeExportLogoFile;

  @ALocalSettingsPath(key = "projectforge.support.mail", defaultValue = "")
  private String projectforgeSupportMail;

  @ALocalSettingsPath(key = "server.port", defaultValue = "8080")
  private String serverPort;

  @ALocalSettingsPath(key = "server.address", defaultValue = "")
  private String serverAddress;

  @Override
  public void validate(ValContext ctx)
  {
    File f = new File(baseDir);
    if (f.exists() == true && f.isDirectory() == false) {
      ctx.directError("baseDir", "Base Dir has to be a directory not file");
      return;
    }
    if (f.exists() == false) {
      boolean mkdirsuc = f.mkdirs();
      // TODO continue;
    }
  }

  @Override
  public LocalSettingsWriter toProperties(LocalSettingsWriter writer)
  {
    super.toProperties(writer);
    LocalSettingsWriter sw = writer.newSection(" \"HttpOnly\" flag for the session cookie.");
    sw.put("server.session.cookie.http-only", "true");
    sw.put("server.session.tracking-modes", "cookie", " this avoids session fixation via jsessionid in URL");
    sw.put("server.session.timeout", "3600", " Session timeout in seconds.");
    sw.put("multipart.maxFileSize", "1024Kb");

    sw.put("pf.config.security.teamCalCryptPassword", "enter-a-password-here",
        " password to encrypt the links which are sent by email for event invitations, max 32 characters");

    sw.put("projectforge.servletContextPath", "${genome.jetty.contextpath}");
    sw.put("projectforge.security.passwordPepper", "*******SECRET********");
    sw.put("projectforge.security.sqlConsoleAvailable", "false");

    sw.put("projectforge.telephoneSystemUrl", "http://asterisk.yourserver.org/originatecall.php?source=#source&target=#target");
    sw.put("projectforge.telephoneSystemNumber", "0123456789");
    sw.put("projectforge.smsUrl", "http://asterisk.yourserver.org/sms.php?number=#number&message=#message");
    sw.put("projectforge.receiveSmsKey", "*******SECRET********");
    sw.put("projectforge.phoneLookupKey", "*******SECRET********");
    sw.put("projectforge.keystoreFile", "jssecacerts");
    sw.put("projectforge.keystorePassphrase", "*******SECRET********");

    sw.put("projectforge.sendMail.charset", "");

    sw.put("projectforge.testsystemColor", "#ff6868");

    sw.put("projectforge.ldap.server", "");
    sw.put("projectforge.ldap.baseDN", "");
    sw.put("projectforge.ldap.managerUser", "");
    sw.put("projectforge.ldap.managerPassword", "");
    sw.put("projectforge.ldap.port", "");
    sw.put("projectforge.ldap.sslCertificateFile", "");
    sw.put("projectforge.ldap.groupBase", "");
    sw.put("projectforge.ldap.userBase", "");
    sw.put("projectforge.ldap.authentication", "");
    sw.put("projectforge.ldap.posixAccountsDefaultGidNumber", "");
    sw.put("projectforge.ldap.sambaAccountsSIDPrefix", "");
    sw.put("projectforge.ldap.sambaAccountsPrimaryGroupSID", "");
    return writer;
  }

  public String getBaseDir()
  {
    return baseDir;
  }

  public void setBaseDir(String baseDir)
  {
    this.baseDir = baseDir;
  }

  public String getProjectforgeDomain()
  {
    return projectforgeDomain;
  }

  public void setProjectforgeDomain(String projectforgeDomain)
  {
    this.projectforgeDomain = projectforgeDomain;
  }

  public String getProjectforgeWicketDevelopmentMode()
  {
    return projectforgeWicketDevelopmentMode;
  }

  public void setProjectforgeWicketDevelopmentMode(String projectforgeWicketDevelopmentMode)
  {
    this.projectforgeWicketDevelopmentMode = projectforgeWicketDevelopmentMode;
  }

  public String getProjectforgeTestsystemMode()
  {
    return projectforgeTestsystemMode;
  }

  public void setProjectforgeTestsystemMode(String projectforgeTestsystemMode)
  {
    this.projectforgeTestsystemMode = projectforgeTestsystemMode;
  }

  public String getProjectforgeLoginHandler()
  {
    return projectforgeLoginHandler;
  }

  public void setProjectforgeLoginHandler(String projectforgeLoginHandler)
  {
    this.projectforgeLoginHandler = projectforgeLoginHandler;
  }

  public String getHibernateSearchDefaultIndexBase()
  {
    return hibernateSearchDefaultIndexBase;
  }

  public void setHibernateSearchDefaultIndexBase(String hibernateSearchDefaultIndexBase)
  {
    this.hibernateSearchDefaultIndexBase = hibernateSearchDefaultIndexBase;
  }

  public String getProjectForgeResourceDirectory()
  {
    return projectForgeResourceDirectory;
  }

  public void setProjectForgeResourceDirectory(String projectForgeResourceDirectory)
  {
    this.projectForgeResourceDirectory = projectForgeResourceDirectory;
  }

  public String getProjectFontsDirectory()
  {
    return projectFontsDirectory;
  }

  public void setProjectFontsDirectory(String projectFontsDirectory)
  {
    this.projectFontsDirectory = projectFontsDirectory;
  }

  public String getProjectForgeLogoFile()
  {
    return projectForgeLogoFile;
  }

  public void setProjectForgeLogoFile(String projectForgeLogoFile)
  {
    this.projectForgeLogoFile = projectForgeLogoFile;
  }

  public String getProjectForgeExportLogoFile()
  {
    return projectForgeExportLogoFile;
  }

  public void setProjectForgeExportLogoFile(String projectForgeExportLogoFile)
  {
    this.projectForgeExportLogoFile = projectForgeExportLogoFile;
  }

  public String getProjectforgeSupportMail()
  {
    return projectforgeSupportMail;
  }

  public void setProjectforgeSupportMail(String projectforgeSupportMail)
  {
    this.projectforgeSupportMail = projectforgeSupportMail;
  }

  public String getServerPort()
  {
    return serverPort;
  }

  public void setServerPort(String serverPort)
  {
    this.serverPort = serverPort;
  }

  public String getServerAddress()
  {
    return serverAddress;
  }

  public void setServerAddress(String serverAddress)
  {
    this.serverAddress = serverAddress;
  }

}
