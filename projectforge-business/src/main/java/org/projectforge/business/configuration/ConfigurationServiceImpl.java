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

package org.projectforge.business.configuration;

import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.meb.MebMailClient;
import org.projectforge.business.orga.ContractType;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.*;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.time.TimeNotation;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.mail.SendMailConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.util.*;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory
          .getLogger(ConfigurationServiceImpl.class);

  private static transient final Set<String> nonExistingResources = new HashSet<>();

  private static transient final Set<String> existingResources = new HashSet<>();

  private static String staticApplicationHomeDir;

  /**
   * Available early in the spring start up phase. Usable by Flyway.
   */
  public static String getStaticApplicationHomeDir() {
    return staticApplicationHomeDir;
  }

  private ConfigXml configXml;

  private final static String DEFAULT_RESOURCES_DIR = "resources";

  private final static String DEFAULT_FONTS_DIR = DEFAULT_RESOURCES_DIR + File.separator + "fonts";

  @Autowired
  private ConfigurationDao configDao;

  @Value("${projectforge.base.dir}")
  private String applicationHomeDir;

  @Value("${projectforge.resourcesDirectory}")
  private String resourceDir;

  private SSLSocketFactory usersSSLSocketFactory;

  @Value("${projectforge.keystoreFile}")
  private String keystoreFile;

  @Value("${projectforge.keystorePassphrase}")
  private String keystorePassphrase;

  @Autowired
  private SendMailConfig sendMailConfiguration;

  @Autowired
  private MebMailClient mebMailClient;

  @Autowired
  private TeamCalCache teamCalCache;

  @Value("${projectforge.fontsDirectory}")
  private String fontsDirectory;

  @Value("${projectforge.telephoneSystemUrl}")
  private String telephoneSystemUrl;

  @Value("${projectforge.telephoneSystemNumber}")
  private String telephoneSystemNumber;

  @Value("${projectforge.receiveSmsKey}")
  private String receiveSmsKey;

  @Value("${projectforge.phoneLookupKey}")
  private String phoneLookupKey;

  @Autowired
  private SecurityConfig securityConfig;

  @Value("${projectforge.servletContextPath}")
  private String servletContextPath;

  @Value("${projectforge.logoFile}")
  private String logoFile;

  @Value("${projectforge.currencySymbol}")
  private String currencySymbol;

  @Value("${projectforge.domain}")
  private String domain;

  @Value("${projectforge.defaultLocale}")
  private Locale defaultLocale;

  @Value("${projectforge.defaultTimeNotation}")
  private TimeNotation defaultTimeNotation;

  @Value("${projectforge.defaultFirstDayOfWeek}")
  private int defaultFirstDayOfWeek;

  @Value("${projectforge.excelPaperSize}")
  private String excelPaperSize;

  @Value("${projectforge.wicket.developmentMode}")
  private boolean developmentMode;

  @Value("${projectforge.support.mail}")
  private String pfSupportMail;

  @Value("${mail.session.pfmailsession.emailEnabled}")
  private String pfmailsessionEmailEnabled;

  @Value("${mail.session.pfmailsession.name}")
  private String pfmailsessionName;

  @Value("${mail.session.pfmailsession.standardEmailSender}")
  private String pfmailsessionStandardEmailSender;

  @Value("${mail.session.pfmailsession.encryption}")
  private String pfmailsessionEncryption;

  @Value("${mail.session.pfmailsession.smtp.host}")
  private String pfmailsessionHost;

  @Value("${mail.session.pfmailsession.smtp.port}")
  private String pfmailsessionPort;

  @Value("${mail.session.pfmailsession.smtp.auth}")
  private boolean pfmailsessionAuth;

  @Value("¢{mail.session.pfmailsession.smtp.user}")
  private String pfmailsessionUser;

  @Value("¢{mail.session.pfmailsession.smtp.password}")
  private String pfmailsessionPassword;

  @Value("${pf.config.security.sqlConsoleAvailable:false}")
  private boolean sqlConsoleAvailable;

  @Value("${pf.config.security.teamCalCryptPassword}")
  private String teamCalCryptPassword;

  @Value("${pf.config.compileCss:true}")
  private boolean compileCss;

  @Value("${projectforge.login.handlerClass}")
  private String loginHandlerClass;

  @Value("${projectforge.max-file-size.image}")
  private String maxFileSizeImage;

  @Value("${projectforge.max-file-size.datev}")
  private String maxFileSizeDatev;

  @Value("${projectforge.max-file-size.xml-dump-import}")
  private String maxFileSizeXmlDumpImport;

  @PostConstruct
  public void init() {
    ConfigurationServiceAccessor.setConfigurationService(this);
    staticApplicationHomeDir = this.applicationHomeDir;
    this.configXml = new ConfigXml(this.applicationHomeDir);
    if (StringUtils.isBlank(this.resourceDir)) {
      this.resourceDir = DEFAULT_RESOURCES_DIR;
    }
    this.resourceDir = FileHelper.getAbsolutePath(applicationHomeDir, this.resourceDir);
    ensureDir(new File(resourceDir));
    if (StringUtils.isBlank(this.fontsDirectory)) {
      this.fontsDirectory = DEFAULT_FONTS_DIR;
    }
    this.fontsDirectory = FileHelper.getAbsolutePath(applicationHomeDir, this.fontsDirectory);
    ensureDir(new File(fontsDirectory));

    setupKeyStores();

    GlobalConfiguration.createConfiguration(this);
  }

  /**
   * Tries to get the given filename from the application's resource dir (file system). If not exist, the content will
   * be taken as resource input stream. Calls getInputStream(filename) and converts input stream to String.
   *
   * @param filename Filename (can include relative path settings): "test.xsl", "fo-styles/doit.xsl".
   * @return Object[2]: First value is the content as string and second value is the url in external form.
   * @see #getResourceAsInputStream(String)
   */
  @Override
  public Object[] getResourceContentAsString(final String filename) {
    final Object[] result = getResourceAsInputStream(filename);
    final InputStream is = (InputStream) result[0];
    if (is != null) {
      try {
        result[0] = IOUtils.toString(is, "UTF-8");
      } catch (final IOException ex) {
        log.error(ex.getMessage(), ex);
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
    return result;
  }

  /**
   * PLEASE NOTE: Don't forget to close the returned InputStream for avoiding leaked resources!!!<br>
   * Tries to get the given filename from the application's resource dir (file system). If not exist, the input stream
   * will be taken as resource input stream.
   *
   * @param filename Filename (can include relative path settings): "test.xsl", "fo-styles/doit.xsl".
   * @return Object[2]: First value is the InputStream and second value is the url in external form.
   */
  @Override
  public Object[] getResourceAsInputStream(final String filename) {
    InputStream is = null;
    String path = null;
    final File base = new File(getResourceDir());
    if (base.isDirectory() == true) {
      final File file = new File(base, filename);
      if (file.exists() == false) {
        showNonExistingMessage(file, false);
      } else {
        try {
          is = new FileInputStream(file);
          path = file.toURI().toString();
        } catch (final FileNotFoundException ex) {
          log.error(file.getAbsoluteFile() + ": " + ex.getMessage(), ex); // Should not occur.
          is = null;
        }
        showExistingMessage(file, false);
      }
    }
    if (is == null) {
      final ClassLoader cLoader = getClass().getClassLoader();
      final URL url = cLoader.getResource(filename);
      if (url != null) {
        path = url.toExternalForm();
      }
      is = cLoader.getResourceAsStream(filename);
    }
    if (is == null) {
      log.error("File '" + filename + "' not found (wether in file system under '" + base.getAbsolutePath()
              + "' nor in resource!)");
    }
    final Object[] result = new Object[2];
    result[0] = is;
    result[1] = path;
    return result;
  }

  /**
   * Resource directory relative to application's home (default 'resources').
   */
  @Override
  public String getResourceDir() {
    return resourceDir;
  }

  /**
   * @return true if at least a send mail host is given, otherwise false.
   */
  @Override
  public boolean isSendMailConfigured() {
    return sendMailConfiguration != null && sendMailConfiguration.isMailSendConfigOk();
  }

  @Override
  public SendMailConfig getSendMailConfiguration() {
    if (isSendMailConfigured()) {
      return sendMailConfiguration;
    }
    return null;
  }

  /**
   * Default value: "resources/fonts" (absolute path).
   *
   * @return the fontsDirectory
   */
  @Override
  public String getFontsDir() {
    return fontsDirectory;
  }

  /**
   * Format http://asterisk.acme.com/originatecall.php?source=#source&amp;target=#target<br/>
   * #source will be replaced by the current user's phone and #target by the chosen phone number to call.
   */
  @Override
  public String getTelephoneSystemUrl() {
    return telephoneSystemUrl;
  }

  @Override
  public boolean isTelephoneSystemUrlConfigured() {
    return StringUtils.isNotEmpty(this.telephoneSystemUrl);
  }

  @Override
  public List<ContractType> getContractTypes() {
    return configXml.getContractTypes();
  }

  /**
   * @return the securityConfig
   */
  @Override
  public SecurityConfig getSecurityConfig() {
    return securityConfig;
  }

  @Override
  public boolean isSecurityConfigured() {
    return securityConfig != null && StringUtils.isNotBlank(securityConfig.getPasswordPepper());
  }

  /**
   * The servlet's context path, "/ProjectForge" at default. You should configure another context path such as "/" if
   * the ProjectForge app runs in another context, such as root context.
   */
  @Override
  public String getServletContextPath() {
    if (StringUtils.isBlank(servletContextPath) == false) {
      return servletContextPath;
    } else {
      return "";
    }
  }

  /**
   * If configured then this logo file is used for displaying at the top of the navigation menu.
   *
   * @return The path of the configured logo (relative to the image dir of the application's resource path, at default:
   * '&lt;app-home&gt;/resources/images').
   */
  @Override
  public String getLogoFile() {
    return logoFile;
  }

  /**
   * Default is €
   */
  @Override
  public String getCurrencySymbol() {
    return currencySymbol;
  }

  /**
   * Only given, if the administrator have configured this domain. Otherwise e. g. the ImageCropper uses
   * req.getHttpServletRequest().getScheme() + "://" + req.getHttpServletRequest().getLocalName() + ":" +
   * req.getHttpServletRequest().getLocalPort()
   *
   * @return domain (host) in form https://www.acme.de:8443/
   */
  @Override
  public String getDomain() {
    return domain;
  }

  /**
   * @return The domain + context path, e.g. https://www.projectforge.org/demo or https://www.acme.com/ProjectForge.
   */
  @Override
  public String getPfBaseUrl() {
    return getDomain() + getServletContextPath();
  }

  /**
   * For direct calls all numbers beginning with the this number will be stripped, e. g. for 0561316793: 056131679323 ->
   * 23. So internal calls are supported.
   */
  @Override
  public String getTelephoneSystemNumber() {
    return telephoneSystemNumber;
  }

  /**
   * The SMS receiver verifies this key given as get parameter to the servlet call. <br/>
   * The key should be an alpha numeric random value with at least 6 characters for security reasons.
   */
  @Override
  public String getReceiveSmsKey() {
    return receiveSmsKey;
  }

  /**
   * The reverse phone lookup service verifies the key given as parameter to the servlet call against this key. The key
   * should be an alpha numeric random value with at least 6 characters for security reasons.
   *
   * @return the receivePhoneLookupKey
   */
  @Override
  public String getPhoneLookupKey() {
    return phoneLookupKey;
  }

  /**
   * For additional certificates you can set the file name of the jssecert file in your ProjectForge home (config)
   * directory (path of your confix.xml). <br/>
   * If given then the key-store file is used.
   */
  @Override
  public String getKeystoreFile() {
    return keystoreFile;
  }

  @Override
  public SSLSocketFactory getUsersSSLSocketFactory() {
    return usersSSLSocketFactory;
  }

  /**
   * @return true if meb mail account with hostname is configured, otherwise false.
   */
  @Override
  public boolean isMebMailAccountConfigured() {
    return mebMailClient.isMailAccountAvailable();
  }

  private void setupKeyStores() {
    if (StringUtils.isBlank(getKeystoreFile()) == false) {
      try {
        File keystoreFile = new File(getKeystoreFile());
        if (keystoreFile.canRead() == false) {
          keystoreFile = new File(applicationHomeDir, getKeystoreFile());
        }
        if (keystoreFile.canRead() == false) {
          log.warn("Can't read keystore file: " + getKeystoreFile());
          return;
        }
        final InputStream is = new FileInputStream(keystoreFile);
        usersSSLSocketFactory = createSSLSocketFactory(is, this.keystorePassphrase);
        log.info("Keystore successfully read from file: " + keystoreFile.getAbsolutePath());
      } catch (final Throwable ex) {
        log.error("Could not initialize your key store (see error message below)!");
        log.error(ex.getMessage(), ex);
      }
    }
  }

  private SSLSocketFactory createSSLSocketFactory(final InputStream is, final String passphrase) throws Exception {
    final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(is, passphrase.toCharArray());
    is.close();
    final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);
    final X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
    final SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, new TrustManager[]{defaultTrustManager}, null);
    return context.getSocketFactory();
  }

  private static void showNonExistingMessage(final File file, final boolean directory) {
    // Synchronized not needed, for concurrent calls, output entries exist twice in the worst case.
    if (nonExistingResources.contains(file.getAbsolutePath()) == false) {
      nonExistingResources.add(file.getAbsolutePath());
      existingResources.remove(file.getAbsolutePath()); // If changed by administrator during application running.
      final String type = directory == true ? "directory" : "file";
      log.info("Using default " + type + " of ProjectForge, because " + type + "'" + file.getAbsolutePath()
              + "' does not exist (OK)");
    }
  }

  private static void showExistingMessage(final File file, final boolean directory) {
    // Synchronized not needed, for concurrent calls, output entries exist twice in the worst case.
    if (existingResources.contains(file.getAbsolutePath()) == false) {
      existingResources.add(file.getAbsolutePath());
      nonExistingResources.remove(file.getAbsolutePath()); // If changed by administrator during application running.
      final String type = directory == true ? "directory" : "file";
      log.info("Using existing " + type + ":" + file.getAbsolutePath());
    }
  }

  private boolean ensureDir(final File dir) {
    if (dir.exists() == false) {
      log.info("Creating directory " + dir);
      dir.mkdir();
    }
    if (dir.canRead() == false) {
      log.error("Can't create directory: " + dir);
      return false;
    }
    return true;
  }

  @Override
  public Object getDaoValue(IConfigurationParam parameter, ConfigurationDO configurationDO) {
    return configDao.getValue(parameter, configurationDO);
  }

  @Override
  public List<ConfigurationDO> daoInternalLoadAll() {
    return configDao.internalLoadAll();
  }

  @Override
  public List<ConfigurationDO> daoInternalLoadAll(TenantDO tenant) {
    return configDao.internalLoadAll(tenant);
  }

  @Override
  public TimeZone getTimezone() {
    ConfigurationDO configurationDO = configDao.getEntry(ConfigurationParam.DEFAULT_TIMEZONE);
    if (configurationDO != null) {
      return configurationDO.getTimeZone();
    } else {
      log.error("No timezone configured in db configuration. Return default timezone.");
      return TimeZone.getDefault();
    }
  }

  @Override
  public MailSessionLocalSettingsConfigModel createMailSessionLocalSettingsConfigModel() {
    return new MailSessionLocalSettingsConfigModel()
            .setEmailEnabled(pfmailsessionEmailEnabled)
            .setName(pfmailsessionName)
            .setEmailHost(pfmailsessionHost)
            .setEmailPort(String.valueOf(pfmailsessionPort))
            .setStandardEmailSender(pfmailsessionStandardEmailSender)
            .setEmailAuthEnabled(String.valueOf(pfmailsessionAuth))
            .setEmailAuthUser(pfmailsessionUser)
            .setEmailAuthPass(pfmailsessionPassword)
            .setEncryption(pfmailsessionEncryption);
  }

  @Override
  public boolean isMultiTenancyConfigured() {
    return GlobalConfiguration.getInstance().isMultiTenancyConfigured();
  }

  @Override
  public String getPfSupportMailAddress() {
    return pfSupportMail;
  }

  @Override
  public String getApplicationHomeDir() {
    return applicationHomeDir;
  }

  @Override
  public boolean isSqlConsoleAvailable() {
    return sqlConsoleAvailable;
  }

  public Locale getDefaultLocale()
  {
    return defaultLocale;
  }

  public TimeNotation getDefaultTimeNotation()
  {
    return defaultTimeNotation;
  }

  @Override
  public int getDefaultFirstDayOfWeek() {
    return defaultFirstDayOfWeek;
  }

  @Override
  public String getExcelPaperSize() {
    return excelPaperSize;
  }

  @Override
  public boolean getCompileCss() {
    return compileCss;
  }

  @Override
  public String getLoginHandlerClass() {
    return loginHandlerClass;
  }

  @Override
  public String getTeamCalCryptPassword() {
    return teamCalCryptPassword;
  }

  @Override
  public Calendar getEndDateVacationFromLastYear() {
    int day = 31;
    int month = 2;
    ConfigurationDO configDO = configDao.getEntry(ConfigurationParam.END_DATE_VACATION_LASTR_YEAR);
    if (configDO != null) {
      String dayMonthString = configDO.getStringValue();
      String[] dayMonthParts = dayMonthString.split("\\.");
      try {
        month = Integer.parseInt(dayMonthParts[1]) - 1;
        day = Integer.parseInt(dayMonthParts[0]);
      } catch (NumberFormatException e) {
        log.error("Error while parsing ConfigurationParam.END_DATE_VACATION_LASTR_YEAR: " + dayMonthString);
        day = 31;
        month = 2;
      }
    }
    Calendar result = Calendar.getInstance(ThreadLocalUserContext.getTimeZone());
    result.set(result.get(Calendar.YEAR), month, day, 23, 59, 59);
    return result;
  }

  @Override
  public String getHREmailadress() {
    ConfigurationDO hrMailaddress = configDao.getEntry(ConfigurationParam.HR_MAILADDRESS);
    if (hrMailaddress != null) {
      return hrMailaddress.getStringValue();
    }
    return null;
  }

  @Override
  public TeamCalDO getVacationCalendar() {
    return teamCalCache.getCalendar((Integer) configDao.getValue(ConfigurationParam.VACATION_CAL_ID));
  }

  @Override
  public int getMinPasswordLength() {
    try {
      final ConfigurationDO minPwLenEntry = configDao.getEntry(ConfigurationParam.MIN_PASSWORD_LENGTH);
      if (minPwLenEntry != null) {
        final Integer minPwLenValue = minPwLenEntry.getIntValue();
        if (minPwLenValue != null) {
          return minPwLenValue;
        }
      }
    } catch (final RuntimeException e) {
      // this could happen if the database is not initialized (during projectforge initial setup)
      log.warn("Exception while getting the min password length configuration.", e);
    }
    return ConfigurationParam.MIN_PASSWORD_LENGTH.getDefaultIntValue();
  }

  @Override
  public boolean getFlagCheckPasswordChange() {
    try {
      final ConfigurationDO flagCheckPwChangeConf = configDao.getEntry(ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE);
      if (flagCheckPwChangeConf != null) {
        final Boolean flagCheckPwChange = flagCheckPwChangeConf.getBooleanValue();
        if (flagCheckPwChange != null) {
          return flagCheckPwChange;
        }
      }
    } catch (final RuntimeException e) {
      // this could happen if the database is not initialized (during projectforge initial setup)
      log.warn("Exception while getting configuration flag - password change requirement.", e);
    }
    return ConfigurationParam.PASSWORD_FLAG_CHECK_CHANGE.getDefaultBooleanValue();
  }

  @Override
  public String getMaxFileSizeImage() {
    return this.maxFileSizeImage;
  }

  @Override
  public String getMaxFileSizeDatev() {
    return this.maxFileSizeDatev;
  }

  @Override
  public String getMaxFileSizeXmlDumpImport() {
    return this.maxFileSizeXmlDumpImport;
  }

  @Override
  public boolean isSnowEffectEnabled() {
    return GlobalConfiguration.getInstance().getBooleanValue(ConfigurationParam.SNOW_EFFECT_ENABLED);
  }

  void setDefaultLocale(Locale defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  void setDefaultFirstDayOfWeek(int defaultFirstDayOfWeek) {
    this.defaultFirstDayOfWeek = defaultFirstDayOfWeek;
  }
}
