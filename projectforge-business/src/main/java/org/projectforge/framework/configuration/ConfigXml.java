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

package org.projectforge.framework.configuration;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.dom4j.Element;
import org.projectforge.ProjectForgeApp;
import org.projectforge.business.fibu.kost.AccountingConfig;
import org.projectforge.business.orga.ContractType;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.calendar.ConfigureHoliday;
import org.projectforge.framework.utils.FileHelper;
import org.projectforge.framework.xstream.*;
import org.projectforge.jira.JiraConfig;
import org.projectforge.jira.JiraIssueType;
import org.projectforge.storage.StorageConfig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Configure ProjectForge via config.xml in the application's base dir.<br/>
 * The config.xml will never re-read automatically. Please call the web admin page to force a re-read.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XmlObject(alias = "config")
public class ConfigXml {
  private static final String SECRET_PROPERTY_STRING = "******";

  private static transient final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigXml.class);

  private static transient ConfigXml instance;

  private transient final List<ConfigurationListener> listeners = new ArrayList<ConfigurationListener>();

  private String applicationHomeDir;

  private JiraConfig jiraConfig;

  private String jiraBrowseBaseUrl;

  private StorageConfig storageConfig;

  private String telephoneSystemOperatorPanelUrl;

  private List<ConfigureHoliday> holidays;

  private transient File configFile;

  private String databaseDirectory;

  private String loggingDirectory;

  private String workingDirectory;

  private String tempDirectory;

  private List<ContractType> contractTypes;

  //  private MenuEntryConfig menuConfig;
  //
  //  private WebConfig webConfig;

  private boolean portletMode;

  private AccountingConfig accountingConfig;

  /**
   * Separated list of main classes (separated by white chars and or ',').
   */
  String pluginMainClasses;

  // Please note: If you change the name of this member field don't forget to change the PLUGIN_CONFIGS_FIELD_NAME below.
  private List<ConfigurationData> plugins;

  private static final String PLUGIN_CONFIGS_FIELD_NAME = "plugins";

  public static ConfigXml getInstance() {
    if (instance == null) {
      throw new IllegalStateException("Configuration is not yet configured");
    }
    return instance;
  }

  public static boolean isInitialized() {
    return instance != null;
  }

  private void reset() {
    jiraConfig = null;
    jiraBrowseBaseUrl = null;
    telephoneSystemOperatorPanelUrl = null;
    holidays = null;
    databaseDirectory = "database";
    loggingDirectory = "logs";
    workingDirectory = "work";
    tempDirectory = "tmp";
    accountingConfig = new AccountingConfig();
    accountingConfig.reset();
    contractTypes = null;
  }

  protected ConfigXml() {
    reset();
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

  /**
   * Loads the configuration file config.xml from the application's home dir if given, otherwise the default values will
   * be assumed. Constructor is used by Spring instantiation.
   */
  public ConfigXml(final String applicationHomeDir) {
    this.applicationHomeDir = applicationHomeDir;
    log.info("Using application home dir: " + applicationHomeDir);
    //    System.setProperty("base.dir", applicationHomeDir); // Needed by log4j
    final File dir = new File(this.applicationHomeDir);
    final boolean status = ensureDir(dir);
    if (status == true) {
      readConfiguration();
      this.databaseDirectory = FileHelper.getAbsolutePath(applicationHomeDir, this.databaseDirectory);
      ensureDir(new File(databaseDirectory));
      this.loggingDirectory = FileHelper.getAbsolutePath(applicationHomeDir, this.loggingDirectory);
      ensureDir(new File(loggingDirectory));
      this.workingDirectory = FileHelper.getAbsolutePath(applicationHomeDir, this.workingDirectory);
      ensureDir(new File(workingDirectory));
      this.tempDirectory = FileHelper.getAbsolutePath(applicationHomeDir, this.tempDirectory);
      ensureDir(new File(tempDirectory));
    }
    //    if (menuConfig != null) {
    //      menuConfig.setParents();
    //    }
    instance = this;
  }

  public void register(final ConfigurationListener listener) {
    listeners.add(listener);
  }

  /**
   * Reads the configuration file (can be called after any modification of the config file).
   */
  public String readConfiguration() {
    reset();
    configFile = new File(applicationHomeDir, "config.xml");
    String msg = "";
    if (configFile.canRead() == false) {
      if (!ProjectForgeApp.ensureInitialConfigFile("initialConfig.xml", "config.xml")) {
        msg = "Cannot read from config file: '" + getConfigFilePath() + "'. OK, assuming default values.";
        log.info(msg);
      }
    } else {
      final XmlObjectReader reader = getReader();
      String xml = null;
      try {
        xml = FileUtils.readFileToString(configFile, "UTF-8");
      } catch (final IOException ex) {
        msg = "Cannot read config file '" + getConfigFilePath() + "' properly: " + ex;
        log.error(msg, ex);
      }
      if (xml != null) {
        try {
          final ConfigXml cfg = (ConfigXml) reader.read(xml);
          final String warnings = reader.getWarnings();
          copyDeclaredFields(null, this.getClass(), cfg, this);
          if (CollectionUtils.isNotEmpty(cfg.plugins) == true) {
            for (final ConfigurationData srcData : cfg.plugins) {
              final ConfigurationData destData = this.getPluginConfig(srcData.getClass());
              copyDeclaredFields(destData.getClass().getName() + ".", srcData.getClass(), srcData, destData);
            }
          }
          msg = "Config file '" + getConfigFilePath() + "' successfully read.";
          if (warnings != null) {
            msg += "\n" + warnings;
          }
          log.info(msg);
        } catch (final Throwable ex) {
          msg = "Cannot read config file '" + getConfigFilePath() + "' properly: " + ex;
          log.error(msg, ex);
        }
      }
    }
    for (final ConfigurationListener listener : listeners) {
      listener.afterRead();
    }
    return msg;
  }

  public String exportConfiguration() {
    final XmlObjectWriter writer = new XmlObjectWriter() {
      @Override
      protected boolean ignoreField(final Object obj, final Field field) {
        if (field.getDeclaringClass().isAssignableFrom(ConfigXml.class) == true
                && StringHelper.isIn(field.getName(), "expireTime", "timeOfLastRefresh") == true) {
          return true;
        }
        return super.ignoreField(obj, field);
      }

      /**
       * @see org.projectforge.framework.xstream.XmlObjectWriter#writeField(java.lang.reflect.Field, java.lang.Object,
       *      java.lang.Object, org.projectforge.framework.xstream.XmlField, org.dom4j.Element)
       */
      @Override
      protected void writeField(final Field field, final Object obj, final Object fieldValue, final XmlField annotation,
                                final Element element) {
        if (field != null) {
          if (field.isAnnotationPresent(ConfigXmlSecretField.class) == true) {
            super.writeField(field, obj, SECRET_PROPERTY_STRING, annotation, element);
            return;
          }
        }
        super.writeField(field, obj, fieldValue, annotation, element);
      }
    };
    final String xml = writer.writeToXml(this, true);
    return XmlHelper.XML_HEADER + xml;
  }

  private static XmlObjectReader getReader() {
    final XmlObjectReader reader = new XmlObjectReader();
    final AliasMap aliasMap = new AliasMap();
    reader.setAliasMap(aliasMap);
    reader.initialize(ConfigXml.class);
    reader.initialize(ConfigureHoliday.class);
    reader.initialize(ContractType.class);
    reader.initialize(JiraIssueType.class);
    AccountingConfig.registerXmlObjects(reader, aliasMap);
    return reader;
  }

  /**
   * For test cases.
   *
   * @param config
   */
  static void internalSetInstance(final String config) {
    final XmlObjectReader reader = getReader();
    final ConfigXml cfg = (ConfigXml) reader.read(config);
    instance = new ConfigXml();
    copyDeclaredFields(null, instance.getClass(), cfg, instance);
  }

  /**
   * Copies only not null values of the configuration.
   */
  private static void copyDeclaredFields(final String prefix, final Class<?> srcClazz, final Object src,
                                         final Object dest,
                                         final String... ignoreFields) {
    final Field[] fields = srcClazz.getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    for (final Field field : fields) {
      if (ignoreFields != null && ArrayUtils.contains(ignoreFields, field.getName()) == false && accept(field)) {
        try {
          final Object srcFieldValue = field.get(src);
          if (srcFieldValue == null) {
            // Do nothing
          } else if (srcFieldValue instanceof ConfigurationData) {
            final Object destFieldValue = field.get(dest);
            Validate.notNull(destFieldValue);
            final StringBuffer buf = new StringBuffer();
            if (prefix != null) {
              buf.append(prefix);
            }
            String alias = null;
            if (field.isAnnotationPresent(XmlField.class)) {
              final XmlField xmlFieldAnn = field.getAnnotation(XmlField.class);
              if (xmlFieldAnn != null) {
                alias = xmlFieldAnn.alias();
              }
            }
            if (alias != null) {
              buf.append(alias);
            } else {
              buf.append(field.getClass().getName());
            }
            buf.append(".");
            copyDeclaredFields(buf.toString(), srcFieldValue.getClass(), srcFieldValue, destFieldValue, ignoreFields);
          } else if (PLUGIN_CONFIGS_FIELD_NAME.equals(field.getName()) == true) {
            // Do nothing.
          } else {
            field.set(dest, srcFieldValue);
            if (field.isAnnotationPresent(ConfigXmlSecretField.class) == true) {
              log.info(StringUtils.defaultString(prefix) + field.getName() + " = " + SECRET_PROPERTY_STRING);
            } else {
              log.info(StringUtils.defaultString(prefix) + field.getName() + " = " + srcFieldValue);
            }
          }
        } catch (final IllegalAccessException ex) {
          throw new InternalError("Unexpected IllegalAccessException: " + ex.getMessage());
        }
      }
    }
    final Class<?> superClazz = srcClazz.getSuperclass();
    if (superClazz != null) {
      copyDeclaredFields(prefix, superClazz, src, dest, ignoreFields);
    }
  }

  /**
   * Returns whether or not to append the given <code>Field</code>.
   * <ul>
   * <li>Ignore transient fields
   * <li>Ignore static fields
   * <li>Ignore inner class fields</li>
   * </ul>
   *
   * @param field The Field to test.
   * @return Whether or not to consider the given <code>Field</code>.
   */
  protected static boolean accept(final Field field) {
    if (field.getName().indexOf(ClassUtils.INNER_CLASS_SEPARATOR_CHAR) != -1) {
      // Reject field from inner class.
      return false;
    }
    if (Modifier.isTransient(field.getModifiers()) == true) {
      // transients.
      return false;
    }
    if (Modifier.isStatic(field.getModifiers()) == true) {
      // transients.
      return false;
    }
    return true;
  }

  /**
   * Base url for linking JIRA issues: https://jira.acme.com/jira/browse/PROJECTFORGE-222. The issue name
   * UPPERCASE_LETTERS-### will be appended to this url. ProjectForge parses the user's text input for
   * [A-Z][A-Z0-9*]-[0-9]* and displays a list of detected JIRA-issues with a link beside the text area containing such
   * issues.<br/>
   * Example: https://jira.acme.com/jira/browse/ (don't forget closing '/'). <br/>
   * If null then no text input will be parsed and no JIRA link will be displayed.
   */
  public String getJiraBrowseBaseUrl() {
    return jiraBrowseBaseUrl;
  }

  /**
   * FOR INTERNAL USE ONLY (tests). Please configure this value via config.xml.
   *
   * @param jiraBrowseBaseUrl
   */
  public void setJiraBrowseBaseUrl(final String jiraBrowseBaseUrl) {
    this.jiraBrowseBaseUrl = jiraBrowseBaseUrl;
  }

  public JiraConfig getJiraConfig() {
    return jiraConfig;
  }

  /**
   * @return true if a JIRA browse base url is given.
   */
  public final boolean isJIRAConfigured() {
    return StringUtils.isNotBlank(getJiraBrowseBaseUrl());
  }

  /**
   * @return the storageConfig
   */
  public StorageConfig getStorageConfig() {
    return storageConfig;
  }

  public boolean isStorageConfigured() {
    return storageConfig != null && StringUtils.isNotBlank(storageConfig.getAuthenticationToken());
  }

  public String getTelephoneSystemOperatorPanelUrl() {
    return telephoneSystemOperatorPanelUrl;
  }

  public List<ContractType> getContractTypes() {
    return contractTypes;
  }

  /**
   * @return the databaseDirectory
   */
  public String getDatabaseDirectory() {
    return databaseDirectory;
  }

  /**
   * @param databaseDirectory the databaseDirectory to set absolute or relative to the application's home dir.
   * @return this for chaining.
   */
  public void setDatabaseDirectory(final String databaseDirectory) {
    this.databaseDirectory = databaseDirectory;
  }

  /**
   * @return the loggingDirectory
   */
  public String getLoggingDirectory() {
    return loggingDirectory;
  }

  /**
   * @param loggingDirectory the loggingDirectory to set absolute or relative to the application's home dir.
   * @return this for chaining.
   */
  public void setLoggingDirectory(final String loggingDirectory) {
    this.loggingDirectory = loggingDirectory;
  }

  /**
   * This directory is used for e. g. storing uploaded files. The absolute path will be returned. <br/>
   * Default value: "work"
   *
   * @see #setWorkingDirectory(String)
   */
  public String getWorkingDirectory() {
    return workingDirectory;
  }

  /**
   * Sets the working dir as relative sub directory of the application's home dir or the absolute path if given.
   *
   * @param workingDirectory
   */
  public void setWorkingDirectory(final String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  /**
   * This directory is used e. g. by the ImageCropper. The absolute path will be returned. <br/>
   * Default value: "tmp"
   *
   * @see #setWorkingDirectory(String)
   */
  public String getTempDirectory() {
    return tempDirectory;
  }

  /**
   * Sets the temporary dir as relative sub directory of the application's home dir or the absolute path if given. This
   * directory is used by ProjectForge to save temporary files such as images from the ImageCropper.
   *
   * @param tempDirectory
   */
  public void setTempDirectory(final String tempDirectory) {
    this.tempDirectory = tempDirectory;
  }

  public String getConfigFilePath() {
    return configFile.getPath();
  }

  public List<ConfigureHoliday> getHolidays() {
    return holidays;
  }

  /**
   * Here you can define a list of main classes of type AbstractPlugin. These classes will be initialized on startup.
   * Multiple entries should be separated by white chars and/or ','.
   *
   * @return
   */
  public String[] getPluginMainClasses() {
    return StringUtils.split(pluginMainClasses, " \r\n\t,");
  }

  /**
   * If no such plugin config exist, a new instance is created and returned.
   *
   * @return the pluginConfigs
   */
  public ConfigurationData getPluginConfig(final Class<? extends ConfigurationData> configClass) {
    if (plugins == null) {
      plugins = new ArrayList<ConfigurationData>();
    } else {
      for (final ConfigurationData configData : plugins) {
        if (configData != null && configClass.isAssignableFrom(configData.getClass()) == true) {
          return configData;
        }
      }
    }
    final ConfigurationData config = (ConfigurationData) BeanHelper.newInstance(configClass);
    plugins.add(config);
    return config;
  }

  /**
   * Here you can add menu entries to be hidden or can build your own menu tree or just modify the existing one. If you
   * don't configure this element, you will receive the standard ProjectForge menu containing all menu entries which are
   * available for the system and the user. <br/>
   * Please note: ProjectForge assures, that only such menu entries are visible, to which the user has the access to
   * (independant from your definitions here)! <br/>
   * If you want to make a menu entry invisible, you can add this to this root element like this:<br/>
   *
   * <pre>
   * &lt;menu-entry id="DEVELOPER_DOC" visible="false"/&gt; <br/>
   * See all the predefined id's here: {@link MenuItemDef} <br/>
   * This root element will not be shown.
   */
  //  public MenuEntryConfig getMenuConfig()
  //  {
  //    return menuConfig;
  //  }

  /**
   * @return the webConfig
   * @see WebConfig
   */
  //  public WebConfig getWebConfig()
  //  {
  //    return webConfig;
  //  }

  /**
   * Experimental and undocumented setting.
   */
  public boolean isPortletMode() {
    return portletMode;
  }

  /**
   * @return the accountingConfig
   */
  public AccountingConfig getAccountingConfig() {
    return accountingConfig;
  }

  /**
   * Replaces field values with annotation {@link ConfigXmlSecretField} by "******".
   *
   * @param configObject
   * @return String representation of the given object.
   * @see ReflectionToStringBuilder#ReflectionToStringBuilder(Object)
   */
  public static String toString(final Object configObject) {
    return new ReflectionToStringBuilder(configObject) {
      @Override
      protected Object getValue(final Field field) throws IllegalArgumentException, IllegalAccessException {
        if (field.isAnnotationPresent(ConfigXmlSecretField.class) == true) {
          return SECRET_PROPERTY_STRING;
        }
        return super.getValue(field);
      }

    }.toString();
  }
}
