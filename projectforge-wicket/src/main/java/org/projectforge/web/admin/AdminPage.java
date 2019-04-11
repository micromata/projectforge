/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web.admin;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlDumpService;
import de.micromata.genome.util.runtime.RuntimeIOException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.book.BookDO;
import org.projectforge.business.book.BookDao;
import org.projectforge.business.book.BookStatus;
import org.projectforge.business.meb.MebMailClient;
import org.projectforge.business.systeminfo.SystemService;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.business.user.UserXmlPreferencesMigrationDao;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.configuration.GlobalConfiguration;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.api.ReindexSettings;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.history.HibernateSearchReindexer;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.web.WebConfiguration;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.*;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class AdminPage extends AbstractStandardFormPage implements ISelectCallerPage {
  private static final long serialVersionUID = 8345068133036236305L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminPage.class);

  static final int NUMBER_OF_TEST_OBJECTS_TO_CREATE = 100;

  @SpringBean
  private BookDao bookDao;

  @SpringBean
  private JpaXmlDumpService jpaXmlDumpService;

  @SpringBean
  private SystemService systemService;

  @SpringBean
  private DatabaseService myDatabaseUpdater;

  @SpringBean
  private HibernateSearchReindexer hibernateSearchReindexer;

  @SpringBean
  private MebMailClient mebMailClient;

  @SpringBean
  private UserXmlPreferencesCache userXmlPreferencesCache;

  @SpringBean
  private UserXmlPreferencesMigrationDao userXmlPreferencesMigrationDao;

  @SpringBean
  private PfEmgrFactory emf;

  @SpringBean
  MenuCreator menuCreator;

  @SpringBean
  PluginAdminService pluginAdminService;

  private final AdminForm form;

  @Override
  protected void onBeforeRender() {
    super.onBeforeRender();
    checkAccess();
  }

  public AdminPage(final PageParameters parameters) {
    super(parameters);
    form = new AdminForm(this);
    body.add(form);
    form.init();

    addDatabaseActionsMenu();
    addCachesMenu();
    addConfigurationMenu();
    addMEBMenu();
    addMiscMenu();
    addDevelopmentMenu();
  }

  @SuppressWarnings("serial")
  protected void addConfigurationMenu() {
    // Configuration
    final ContentMenuEntryPanel configurationMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            getString("system.admin.group.title.systemChecksAndFunctionality.configuration"));
    addContentMenuEntry(configurationMenu);
    // Check re-read configuration
    final Link<Void> rereadConfigurationLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        rereadConfiguration();
      }
    };
    final ContentMenuEntryPanel rereadConfigurationLinkMenuItem = new ContentMenuEntryPanel(
            configurationMenu.newSubMenuChildId(),
            rereadConfigurationLink, getString("system.admin.button.rereadConfiguration"))
            .setTooltip(getString("system.admin.button.rereadConfiguration.tooltip"));
    configurationMenu.addSubMenuEntry(rereadConfigurationLinkMenuItem);

    // Export configuration.
    final Link<Void> exportConfigurationLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        exportConfiguration();
      }
    };
    final ContentMenuEntryPanel exportConfigurationLinkMenuItem = new ContentMenuEntryPanel(
            configurationMenu.newSubMenuChildId(),
            exportConfigurationLink, getString("system.admin.button.exportConfiguration"))
            .setTooltip(getString("system.admin.button.exportConfiguration.tooltip"));
    configurationMenu.addSubMenuEntry(exportConfigurationLinkMenuItem);
  }

  @SuppressWarnings("serial")
  protected void addCachesMenu() {
    // Caches
    final ContentMenuEntryPanel cachesMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            getString("system.admin.group.title.systemChecksAndFunctionality.caches"));
    addContentMenuEntry(cachesMenu);
    // Refresh caches.
    final Link<Void> refreshCachesLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        refreshCaches();
      }
    };
    final ContentMenuEntryPanel refreshCachesLinkMenuItem = new ContentMenuEntryPanel(cachesMenu.newSubMenuChildId(),
            refreshCachesLink,
            getString("system.admin.button.refreshCaches"))
            .setTooltip(getString("system.admin.button.refreshCaches.tooltip"));
    cachesMenu.addSubMenuEntry(refreshCachesLinkMenuItem);
  }

  @SuppressWarnings("serial")
  protected void addDatabaseActionsMenu() {
    // Data-base actions
    final ContentMenuEntryPanel databaseActionsMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            getString("system.admin.group.title.databaseActions"));
    addContentMenuEntry(databaseActionsMenu);
    // Update all user preferences
    final Link<Void> updateUserPrefsLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        updateUserPrefs();
      }
    };
    final ContentMenuEntryPanel updateUserPrefsLinkMenuItem = new ContentMenuEntryPanel(
            databaseActionsMenu.newSubMenuChildId(),
            updateUserPrefsLink, getString("system.admin.button.updateUserPrefs"))
            .setTooltip(getString("system.admin.button.updateUserPrefs.tooltip"));
    databaseActionsMenu.addSubMenuEntry(updateUserPrefsLinkMenuItem);

    // Create missing data-base indices.
    final Link<Void> createMissingDatabaseIndicesLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        createMissingDatabaseIndices();
      }
    };
    final ContentMenuEntryPanel createMissingDatabaseIndicesLinkMenuItem = new ContentMenuEntryPanel(
            databaseActionsMenu.newSubMenuChildId(), createMissingDatabaseIndicesLink,
            getString("system.admin.button.createMissingDatabaseIndices"))
            .setTooltip(getString("system.admin.button.createMissingDatabaseIndices.tooltip"));
    databaseActionsMenu.addSubMenuEntry(createMissingDatabaseIndicesLinkMenuItem);
    {
      // Dump data-base.
      final Link<Void> dumpDatabaseLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
        @Override
        public void onClick() {
          dump();
        }
      };
      final ContentMenuEntryPanel dumpDatabaseLinkMenuItem = new ContentMenuEntryPanel(
              databaseActionsMenu.newSubMenuChildId(),
              dumpDatabaseLink, getString("system.admin.button.dump"))
              .setTooltip(getString("system.admin.button.dump.tooltip"));
      databaseActionsMenu.addSubMenuEntry(dumpDatabaseLinkMenuItem);
      dumpDatabaseLink.add(WicketUtils.javaScriptConfirmDialogOnClick(getString("system.admin.button.dump.question")));
    }
    {
      // Schema export.
      final Link<Void> schemaExportLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
        @Override
        public void onClick() {
          schemaExport();
        }
      };
      final ContentMenuEntryPanel schemaExportLinkMenuItem = new ContentMenuEntryPanel(
              databaseActionsMenu.newSubMenuChildId(),
              schemaExportLink, getString("system.admin.button.schemaExport"))
              .setTooltip(getString("system.admin.button.schemaExport.tooltip"));
      databaseActionsMenu.addSubMenuEntry(schemaExportLinkMenuItem);
    }
  }

  @SuppressWarnings("serial")
  protected void addMiscMenu() {
    // Misc checks
    final ContentMenuEntryPanel miscChecksMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            getString("system.admin.group.title.systemChecksAndFunctionality.miscChecks"));
    addContentMenuEntry(miscChecksMenu);
    // Check system integrity
    final Link<Void> checkSystemIntegrityLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        checkSystemIntegrity();
      }
    };
    final ContentMenuEntryPanel checkSystemIntegrityLinkMenuItem = new ContentMenuEntryPanel(
            miscChecksMenu.newSubMenuChildId(),
            checkSystemIntegrityLink, getString("system.admin.button.checkSystemIntegrity"))
            .setTooltip(getString("system.admin.button.checkSystemIntegrity.tooltip"));
    miscChecksMenu.addSubMenuEntry(checkSystemIntegrityLinkMenuItem);
  }

  @SuppressWarnings("serial")
  protected void addMEBMenu() {
    if (Configuration.getInstance().isMebConfigured() == false) {
      // Do nothing.
      return;
    }
    // Mobile enterprise blogging
    final ContentMenuEntryPanel mebMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(),
            getString("meb.title.heading"));
    addContentMenuEntry(mebMenu);
    // Check unseen meb mails
    final Link<Void> checkUnseenMebMailsLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        checkUnseenMebMails();
      }
    };
    final ContentMenuEntryPanel checkUnseenMebMailsLinkMenuItem = new ContentMenuEntryPanel(mebMenu.newSubMenuChildId(),
            checkUnseenMebMailsLink, getString("system.admin.button.checkUnseenMebMails"))
            .setTooltip(getString("system.admin.button.checkUnseenMebMails.tooltip"));
    mebMenu.addSubMenuEntry(checkUnseenMebMailsLinkMenuItem);

    // Import all meb mails.
    final Link<Void> importAllMebMailsLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        importAllMebMails();
      }
    };
    final ContentMenuEntryPanel importAllMebMailsLinkMenuItem = new ContentMenuEntryPanel(mebMenu.newSubMenuChildId(),
            importAllMebMailsLink, getString("system.admin.button.importAllMebMails"))
            .setTooltip(getString("system.admin.button.importAllMebMails.tooltip"));
    mebMenu.addSubMenuEntry(importAllMebMailsLinkMenuItem);
  }

  @SuppressWarnings("serial")
  protected void addDevelopmentMenu() {
    if (WebConfiguration.isDevelopmentMode() == false) {
      // Do nothing.
      return;
    }
    // Development actions
    final ContentMenuEntryPanel developmentMenu = new ContentMenuEntryPanel(getNewContentMenuChildId(), "Development");
    addContentMenuEntry(developmentMenu);
    // Check I18n properties.
    final Link<Void> checkI18nPropertiesLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        checkI18nProperties();
      }
    };
    final ContentMenuEntryPanel checkI18nPropertiesLinkMenuItem = new ContentMenuEntryPanel(
            developmentMenu.newSubMenuChildId(),
            checkI18nPropertiesLink, getString("system.admin.button.checkI18nProperties"))
            .setTooltip(getString("system.admin.button.checkI18nProperties.tooltip"));
    developmentMenu.addSubMenuEntry(checkI18nPropertiesLinkMenuItem);
    // Create test objects
    final Link<Void> createTestObjectsLink = new Link<Void>(ContentMenuEntryPanel.LINK_ID) {
      @Override
      public void onClick() {
        createTestBooks();
      }
    };
    createTestObjectsLink.add(WicketUtils.javaScriptConfirmDialogOnClick(getLocalizedMessage(
            "system.admin.development.testObjectsCreationQuestion", AdminPage.NUMBER_OF_TEST_OBJECTS_TO_CREATE, "BookDO")));
    final ContentMenuEntryPanel createTestObjectsLinkMenuItem = new ContentMenuEntryPanel(
            developmentMenu.newSubMenuChildId(),
            createTestObjectsLink, "BookDO").setTooltip("Creates 100 books of type BookDO for testing.");
    developmentMenu.addSubMenuEntry(createTestObjectsLinkMenuItem);
  }

  @Override
  protected String getTitle() {
    return getString("system.admin.title");
  }

  protected void checkUnseenMebMails() {
    log.info("Administration: check for new MEB mails.");
    checkAccess();
    final int counter = mebMailClient.getNewMessages(true, true);
    setResponsePage(new MessagePage("message.successfullCompleted",
            "check for new MEB mails, " + counter + " new messages imported."));
  }

  protected void importAllMebMails() {
    log.info("Administration: import all MEB mails.");
    checkAccess();
    final int counter = mebMailClient.getNewMessages(false, false);
    setResponsePage(new MessagePage("message.successfullCompleted",
            "import all MEB mails, " + counter + " new messages imported."));
  }

  protected void checkSystemIntegrity() {
    log.info("Administration: check integrity of tasks.");
    checkAccess();
    final String result = systemService.checkSystemIntegrity();
    final String filename = "projectforge_check_report" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".txt";
    DownloadUtils.setDownloadTarget(result.getBytes(), filename);
  }

  protected void refreshCaches() {
    log.info("Administration: refresh of caches.");
    checkAccess();
    String refreshedCaches = systemService.refreshCaches();
    userXmlPreferencesCache.forceReload();
    refreshedCaches += ", UserXmlPreferencesCache";
    setResponsePage(new MessagePage("administration.refreshCachesDone", refreshedCaches));
  }

  protected void rereadConfiguration() {
    log.info("Administration: Reload all configurations (DB, XML)");
    checkAccess();
    log.info("Administration: reload global configuration.");
    GlobalConfiguration.getInstance().forceReload();
    log.info("Administration: reload configuration.");
    Configuration.getInstance().forceReload();
    log.info("Administration: reread configuration file config.xml.");
    String result = ConfigXml.getInstance().readConfiguration();
    if (result != null) {
      result = result.replaceAll("\n", "<br/>\n");
    }
    menuCreator.refresh();
    setResponsePage(new MessagePage("administration.rereadConfiguration", result));
  }

  protected void exportConfiguration() {
    log.info("Administration: export configuration file config.xml.");
    checkAccess();
    final String xml = ConfigXml.getInstance().exportConfiguration();
    final String filename = "config-" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".xml";
    DownloadUtils.setUTF8CharacterEncoding(getResponse());
    DownloadUtils.setDownloadTarget(xml.getBytes(), filename);
  }

  protected void checkI18nProperties() {
    log.info("Administration: check i18n properties.");
    checkAccess();
    final StringBuilder buf = new StringBuilder();
    final StringBuilder warnMessages = new StringBuilder();
    final Properties propsFound = new Properties();
    try {
      final ClassLoader cLoader = this.getClass().getClassLoader();
      final InputStream is = cLoader.getResourceAsStream(WebConstants.FILE_I18N_KEYS);
      propsFound.load(is);
    } catch (final IOException ex) {
      log.error("Could not load i18n properties: " + ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
    final SortedMap<String, String> defaultMap = load(warnMessages, "");
    final SortedMap<String, String> deMap = load(warnMessages, "_de");
    buf.append("Checking the differences between the i18n resource properties (default and _de)\n\n");
    buf.append("Found " + defaultMap.size() + " entries in default property file (en).\n\n");
    buf.append("Missing in _de:\n");
    buf.append("---------------\n");
    for (final String key : defaultMap.keySet()) {
      if (deMap.containsKey(key) == false) {
        buf.append(key).append("=").append(defaultMap.get(key)).append("\n");
      }
    }
    buf.append("\n\nOnly in _de (not in _en):\n");
    buf.append("-------------------------\n");
    for (final String key : deMap.keySet()) {
      if (defaultMap.containsKey(key) == false) {
        buf.append(key).append("=").append(deMap.get(key)).append("\n");
      }
    }
    buf.append("\n\nWarnings and errors:\n");
    buf.append("--------------------\n");
    buf.append(warnMessages);
    buf.append("\n\nMaybe not defined but used (found in java, jsp or Wicket's html code):\n");
    buf.append("----------------------------------------------------------------------\n");
    for (final Object key : propsFound.keySet()) {
      if (defaultMap.containsKey(key) == false && deMap.containsKey(key) == false) {
        buf.append(key).append("=").append(propsFound.getProperty((String) key)).append("\n");
      }
    }
    buf.append("\n\nExperimental (in progress): Maybe unused (not found in java, jsp or Wicket's html code):\n");
    buf.append("----------------------------------------------------------------------------------------\n");
    final Set<String> all = new TreeSet<String>();
    CollectionUtils.addAll(all, defaultMap.keySet().iterator());
    CollectionUtils.addAll(all, deMap.keySet().iterator());
    for (final String key : all) {
      if (propsFound.containsKey(key) == true) {
        continue;
      }
      String value = defaultMap.get(key);
      if (value == null) {
        value = deMap.get(key);
      }
      buf.append(key + "=" + value + "\n");
    }
    final String result = buf.toString();
    final String filename = "projectforge_i18n_check" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".txt";
    DownloadUtils.setDownloadTarget(result.getBytes(), filename);
  }

  protected void dump() {
    log.info("Administration: Database dump.");
    checkAccess();
    String ts = DateHelper.getTimestampAsFilenameSuffix(new Date());
    String filename = "projectforgedump_" + ts + ".xml.gz";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (GZIPOutputStream gzout = new GZIPOutputStream(out)) {
      jpaXmlDumpService.dumpToXml(emf, gzout);
    } catch (IOException ex) {
      throw new RuntimeIOException(ex);
    }

    DownloadUtils.setDownloadTarget(out.toByteArray(), filename);
  }

  protected void reindex() {
    log.info("Administration: re-index.");
    checkAccess();
    final ReindexSettings settings = new ReindexSettings(form.reindexFromDate, form.reindexNewestNEntries);
    final String tables = hibernateSearchReindexer.rebuildDatabaseSearchIndices(settings);
    setResponsePage(new MessagePage("administration.databaseSearchIndicesRebuild", tables));
  }

  protected void schemaExport() {
    log.info("Administration: schema export.");
    checkAccess();
    final String result = systemService.exportSchema();
    final String filename = "projectforge_schema" + DateHelper.getDateAsFilenameSuffix(new Date()) + ".sql";
    DownloadUtils.setDownloadTarget(result.getBytes(), filename);
  }

  @Override
  public void cancelSelection(final String property) {
  }

  @Override
  public void select(final String property, final Object selectedValue) {
    if ("reindexFromDate".equals(property) == true) {
      // Date selected.
      final Date date = (Date) selectedValue;
      form.reindexFromDate = date;
      form.reindexFromDatePanel.markModelAsChanged();
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  @Override
  public void unselect(final String property) {
    if ("reindexFromDate".equals(property) == true) {
      form.reindexFromDate = null;
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  protected void formatLogEntries() {
    log.info("Administration: formatLogEntries");
    checkAccess();
    if (form.logEntries == null) {
      form.formattedLogEntries = "";
      return;
    }
    int indent = 0;
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < form.logEntries.length(); i++) {
      final char c = form.logEntries.charAt(i);
      buf.append(c);
      if (c == ',') {
        buf.append("<br/>");
        for (int j = 0; j < indent; j++) {
          buf.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
      } else if (c == '[') {
        indent++;
        buf.append("<br/>");
        for (int j = 0; j < indent; j++) {
          buf.append("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
      } else if (c == ']') {
        indent--;
        buf.append("<br/>");
      }
    }
    form.formattedLogEntries = buf.toString();
  }

  protected void setAlertMessage() {
    log.info("Admin user has set the alert message: \"" + form.alertMessage + "\"");
    checkAccess();
    WicketApplication.setAlertMessage(form.alertMessage);
  }

  protected void clearAlertMessage() {
    log.info("Admin user has cleared the alert message.");
    checkAccess();
    form.alertMessage = null;
    WicketApplication.setAlertMessage(form.alertMessage);
  }

  protected void updateUserPrefs() {
    checkAccess();
    log.info("Administration: updateUserPrefs");
    final String output = userXmlPreferencesMigrationDao.migrateAllUserPrefs();
    final byte[] content = output.getBytes();
    final String ts = DateHelper.getTimestampAsFilenameSuffix(new Date());
    final String filename = "projectforge_updateUserPrefs_" + ts + ".txt";
    DownloadUtils.setDownloadTarget(content, filename);
  }

  protected void createMissingDatabaseIndices() {
    log.info("Administration: create missing data base indices.");
    accessChecker.checkRestrictedOrDemoUser();
    final int counter = myDatabaseUpdater.createMissingIndices();
    setResponsePage(new MessagePage("administration.missingDatabaseIndicesCreated", String.valueOf(counter)));
  }

  private void checkAccess() {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    accessChecker.checkRestrictedOrDemoUser();
  }

  public void createTestBooks() {
    accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    accessChecker.checkRestrictedOrDemoUser();
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    final List<BookDO> list = new ArrayList<BookDO>();
    int number = 1;
    while (myDatabaseUpdater
            .queryForInt("select count(*) from t_book where title like 'title." + number + ".%'") > 0) {
      number++;
    }
    for (int i = 1; i <= NUMBER_OF_TEST_OBJECTS_TO_CREATE; i++) {
      BookDO book = new BookDO();
      book.setTitle(get("title", number, i));
      book.setAbstractText(get("abstractText", number, i));
      book.setAuthors(get("authors", number, i));
      book.setComment(get("comment", number, i));
      book.setEditor(get("editor", number, i));
      book.setIsbn(get("isbn", number, i));
      book.setKeywords(get("keywords", number, i));
      book.setPublisher(get("publisher", number, i));
      book.setSignature(get("signature", number, i));
      book.setStatus(BookStatus.PRESENT);
      book.setYearOfPublishing("2001");
      list.add(book);
    }
    bookDao.save(list);
    setResponsePage(
            new MessagePage("system.admin.development.testObjectsCreated", String.valueOf(NUMBER_OF_TEST_OBJECTS_TO_CREATE),
                    "BookDO"));
  }

  private String get(final String basename, final int number, final int counter) {
    return basename + "." + number + "." + counter;
  }

  public List<String> getResourceBundleNames() {
    final List<String> list = new ArrayList<String>();
    list.addAll(I18nHelper.getBundleNames());
    return list;
  }

  private SortedMap<String, String> load(final StringBuilder warnMessages, final String locale) {
    final ClassLoader cLoader = this.getClass().getClassLoader();
    final SortedMap<String, String> map = new TreeMap<String, String>();
    final List<String> resourceBundleList = getResourceBundleNames();
    for (final String bundle : resourceBundleList) {
      final String path = bundle.replace('.', '/') + locale + ".properties";
      log.info("Loading i18 resource properties: " + path);
      final InputStream is = cLoader.getResourceAsStream(path);
      final Properties properties = new Properties();
      if (is != null) {
        try {
          properties.load(is);
        } catch (final IOException ex) {
          log.error("Error while loading resource properties '" + path + locale + ".properties: " + ex.getMessage(),
                  ex);
          continue;
        }
      }
      for (final Object key : properties.keySet()) {
        final String value = properties.getProperty((String) key);
        if (map.containsKey(key) == true) {
          warnMessages.append("Duplicate entry (locale=").append(locale).append("): ").append(key);
        }
        map.put((String) key, value);
        if (value != null && (value.contains("{0") == true || value.contains("{1") == true)
                && value.contains("'") == true) {
          // Message, check for single quotes:
          char lastChar = ' ';
          for (int i = 0; i < value.length(); i++) {
            final char ch = value.charAt(i);
            if (lastChar == '\'') {
              if (ch != '\'') {
                warnMessages.append("Key '").append(key).append("' (locale=").append(locale)
                        .append(
                                ") contains invalid message string (single quotes are not allowed and must be replaced by '').\n");
                break;
              }
              lastChar = ' '; // Quotes were OK.
            } else {
              lastChar = ch;
            }
          }
        }
      }
      log.info("Found " + map.size() + " entries in: " + path);
    }
    return map;
  }
}
