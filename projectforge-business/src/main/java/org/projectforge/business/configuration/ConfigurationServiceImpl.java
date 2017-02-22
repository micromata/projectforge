package org.projectforge.business.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.projectforge.business.meb.MebMailClient;
import org.projectforge.business.orga.ContractType;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.configuration.IConfigurationParam;
import org.projectforge.framework.configuration.SecurityConfig;
import org.projectforge.framework.configuration.entities.ConfigurationDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.mail.SendMailConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import de.micromata.genome.util.runtime.config.MailSessionLocalSettingsConfigModel;

@Service
public class ConfigurationServiceImpl implements ConfigurationService
{

  private static transient final org.apache.log4j.Logger log = org.apache.log4j.Logger
      .getLogger(ConfigurationServiceImpl.class);

  private static transient final Set<String> nonExistingResources = new HashSet<>();

  private static transient final Set<String> existingResources = new HashSet<>();

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

  @Value("${projectforge.smsUrl}")
  private String smsUrl;

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

  @Value("${projectforge.domain}")
  private String domain;

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

  @PostConstruct
  public void init()
  {
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
  public Object[] getResourceContentAsString(final String filename)
  {
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
  public Object[] getResourceAsInputStream(final String filename)
  {
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
  public String getResourceDir()
  {
    return resourceDir;
  }

  /**
   * @return true if at least a send mail host is given, otherwise false.
   */
  @Override
  public boolean isSendMailConfigured()
  {
    return sendMailConfiguration != null && sendMailConfiguration.isMailSendConfigOk();
  }

  @Override
  public SendMailConfig getSendMailConfiguration()
  {
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
  public String getFontsDir()
  {
    return fontsDirectory;
  }

  /**
   * Format http://asterisk.acme.com/originatecall.php?source=#source&amp;target=#target<br/>
   * #source will be replaced by the current user's phone and #target by the chosen phone number to call.
   */
  @Override
  public String getTelephoneSystemUrl()
  {
    return telephoneSystemUrl;
  }

  @Override
  public boolean isTelephoneSystemUrlConfigured()
  {
    return StringUtils.isNotEmpty(this.telephoneSystemUrl);
  }

  @Override
  public List<ContractType> getContractTypes()
  {
    return configXml.getContractTypes();
  }

  /**
   * @return the securityConfig
   */
  @Override
  public SecurityConfig getSecurityConfig()
  {
    return securityConfig;
  }

  @Override
  public boolean isSecurityConfigured()
  {
    return securityConfig != null && StringUtils.isNotBlank(securityConfig.getPasswordPepper());
  }

  /**
   * The servlet's context path, "/ProjectForge" at default. You should configure another context path such as "/" if
   * the ProjectForge app runs in another context, such as root context.
   */
  @Override
  public String getServletContextPath()
  {
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
  public String getLogoFile()
  {
    return logoFile;
  }

  /**
   * Only given, if the administrator have configured this domain. Otherwise e. g. the ImageCropper uses
   * req.getHttpServletRequest().getScheme() + "://" + req.getHttpServletRequest().getLocalName() + ":" +
   * req.getHttpServletRequest().getLocalPort()
   *
   * @return domain (host) in form https://www.acme.de:8443/
   */
  @Override
  public String getDomain()
  {
    return domain;
  }

  /**
   * @return The domain + context path, e.g. https://www.projectforge.org/demo or https://www.acme.com/ProjectForge.
   */
  @Override
  public String getPfBaseUrl()
  {
    return getDomain() + getServletContextPath();
  }

  /**
   * For direct calls all numbers beginning with the this number will be stripped, e. g. for 0561316793: 056131679323 ->
   * 23. So internal calls are supported.
   */
  @Override
  public String getTelephoneSystemNumber()
  {
    return telephoneSystemNumber;
  }

  @Override
  public boolean isSmsConfigured()
  {
    return StringUtils.isNotEmpty(smsUrl);
  }

  /**
   * Format "http://asterisk.acme.com/sms.php?number=#number&amp;text=#text".<br/>
   * #number will be replaced by the chosen mobile phone number and #text by the sms text (url encoded).
   */
  @Override
  public String getSmsUrl()
  {
    return smsUrl;
  }

  /**
   * The SMS receiver verifies this key given as get parameter to the servlet call. <br/>
   * The key should be an alpha numeric random value with at least 6 characters for security reasons.
   */
  @Override
  public String getReceiveSmsKey()
  {
    return receiveSmsKey;
  }

  /**
   * The reverse phone lookup service verifies the key given as parameter to the servlet call against this key. The key
   * should be an alpha numeric random value with at least 6 characters for security reasons.
   *
   * @return the receivePhoneLookupKey
   */
  @Override
  public String getPhoneLookupKey()
  {
    return phoneLookupKey;
  }

  /**
   * For additional certificates you can set the file name of the jssecert file in your ProjectForge home (config)
   * directory (path of your confix.xml). <br/>
   * If given then the key-store file is used.
   */
  @Override
  public String getKeystoreFile()
  {
    return keystoreFile;
  }

  @Override
  public SSLSocketFactory getUsersSSLSocketFactory()
  {
    return usersSSLSocketFactory;
  }

  /**
   * @return true if meb mail account with hostname is configured, otherwise false.
   */
  @Override
  public boolean isMebMailAccountConfigured()
  {
    return mebMailClient.isMailAccountAvailable();
  }

  private void setupKeyStores()
  {
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

  private SSLSocketFactory createSSLSocketFactory(final InputStream is, final String passphrase) throws Exception
  {
    final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(is, passphrase.toCharArray());
    is.close();
    final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);
    final X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
    final SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, new TrustManager[] { defaultTrustManager }, null);
    return context.getSocketFactory();
  }

  private static void showNonExistingMessage(final File file, final boolean directory)
  {
    // Synchronized not needed, for concurrent calls, output entries exist twice in the worst case.
    if (nonExistingResources.contains(file.getAbsolutePath()) == false) {
      nonExistingResources.add(file.getAbsolutePath());
      existingResources.remove(file.getAbsolutePath()); // If changed by administrator during application running.
      final String type = directory == true ? "directory" : "file";
      log.info("Using default " + type + " of ProjectForge, because " + type + "'" + file.getAbsolutePath()
          + "' does not exist (OK)");
    }
  }

  private static void showExistingMessage(final File file, final boolean directory)
  {
    // Synchronized not needed, for concurrent calls, output entries exist twice in the worst case.
    if (existingResources.contains(file.getAbsolutePath()) == false) {
      existingResources.add(file.getAbsolutePath());
      nonExistingResources.remove(file.getAbsolutePath()); // If changed by administrator during application running.
      final String type = directory == true ? "directory" : "file";
      log.info("Using existing " + type + ":" + file.getAbsolutePath());
    }
  }

  private boolean ensureDir(final File dir)
  {
    if (dir.exists() == false) {
      log.info("Creating directory " + dir);
      dir.mkdir();
    }
    if (dir.canRead() == false) {
      log.fatal("Can't create directory: " + dir);
      return false;
    }
    return true;
  }

  @Override
  public Object getDaoValue(IConfigurationParam parameter, ConfigurationDO configurationDO)
  {
    return configDao.getValue(parameter, configurationDO);
  }

  @Override
  public List<ConfigurationDO> daoInternalLoadAll()
  {
    return configDao.internalLoadAll();
  }

  @Override
  public List<ConfigurationDO> daoInternalLoadAll(TenantDO tenant)
  {
    return configDao.internalLoadAll(tenant);
  }

  @Override
  public TimeZone getTimezone()
  {
    ConfigurationDO configurationDO = configDao.getEntry(ConfigurationParam.DEFAULT_TIMEZONE);
    if (configurationDO != null) {
      return configurationDO.getTimeZone();
    } else {
      log.error("No timezone configured in db configuration. Return default timezone.");
      return TimeZone.getDefault();
    }
  }

  @Override
  public MailSessionLocalSettingsConfigModel createMailSessionLocalSettingsConfigModel()
  {
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
  public boolean isMultiTenancyConfigured()
  {
    return GlobalConfiguration.getInstance().isMultiTenancyConfigured();
  }

  @Override
  public String getPfSupportMailAddress()
  {
    return pfSupportMail;
  }

  @Override
  public String getApplicationHomeDir()
  {
    return applicationHomeDir;
  }

  @Override
  public boolean isSqlConsoleAvailable()
  {
    return sqlConsoleAvailable;
  }

  @Override
  public boolean getCompileCss()
  {
    return compileCss;
  }

  @Override
  public String getLoginHandlerClass()
  {
    return loginHandlerClass;
  }

  @Override
  public String getTeamCalCryptPassword()
  {
    return teamCalCryptPassword;
  }

  @Override
  public Calendar getEndDateVacationFromLastYear()
  {
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
    Calendar now = new GregorianCalendar();
    return new GregorianCalendar(now.get(Calendar.YEAR), month, day);
  }

  @Override
  public String getHREmailadress()
  {
    ConfigurationDO hrMailaddress = configDao.getEntry(ConfigurationParam.HR_MAILADDRESS);
    if (hrMailaddress != null) {
      return hrMailaddress.getStringValue();
    }
    return null;
  }

  @Override
  public TeamCalDO getVacationCalendar()
  {
    return teamCalCache.getCalendar((Integer) configDao.getValue(ConfigurationParam.VACATION_CAL_ID));
  }

}
