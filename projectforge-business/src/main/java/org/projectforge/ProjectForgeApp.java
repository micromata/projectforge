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

package org.projectforge;

import java.util.TimeZone;

import org.projectforge.business.jobs.CronSetup;
import org.projectforge.business.systeminfo.SystemInfoCache;
import org.projectforge.business.user.UserCache;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.export.MyXlsExportContext;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.database.DatabaseCoreInitial;
import org.projectforge.framework.persistence.database.MyDatabaseUpdateService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.registry.Registry;
import org.springframework.context.ApplicationContext;

import net.fortuna.ical4j.util.CompatibilityHints;

/**
 * Doing some initialization stuff and stuff on shutdown (planned). Most stuff is yet done by WicketApplication.
 * 
 * TODO * DESIGNBUG enkoppeln.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */

public class ProjectForgeApp
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProjectForgeApp.class);

  private static ProjectForgeApp instance;

  private boolean upAndRunning;

  private final long startTime = System.currentTimeMillis();

  private ApplicationContext applicationContext;

  private CronSetup cronSetup;

  private MyDatabaseUpdateService myDatabaseUpdater;

  private UserXmlPreferencesCache userXmlPreferencesCache;

  private SystemInfoCache systemInfoCache;

  private boolean initialized;

  public boolean isInitialized()
  {
    return initialized;
  }

  public void setInitialized(boolean initialized)
  {
    this.initialized = initialized;
  }

  public synchronized static ProjectForgeApp init(ApplicationContext applicationContext, final boolean developmentMode)
  {
    if (instance != null) {
      log.warn("ProjectForge is already initialized!");
      return instance;
    }
    instance = new ProjectForgeApp();
    instance.internalInit(applicationContext, developmentMode);
    return instance;
  }

  public static ProjectForgeApp getInstance()
  {
    return instance;
  }

  public static void shutdown()
  {
    if (instance == null) {
      log.error("ProjectForge isn't initialized, can't excecute shutdown!");
      return;
    }
    instance.internalShutdown();
    instance = null;
  }

  /**
   * Should be called on start-up (e. g. by WicketApplication) if all start-up stuff is done and all the services and
   * login should be started. <br>
   * Flag upAndRunning will be set to true.
   */
  public void finalizeInitialization()
  {
    cronSetup.initialize();
    log.info("system cronJobs are initialized.");
    //    TODO RK may be already in PluginAdminService pluginsRegistry.registerCronJobs(cronSetup);
    log.info("plugin cronJobs are initialized.");
    log.info(AppVersion.APP_ID + " " + AppVersion.NUMBER + " (" + AppVersion.RELEASE_TIMESTAMP + ") initialized.");

    // initialize ical4j to be more "relaxed"
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_UNFOLDING, true);
    CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true);
    this.upAndRunning = true;
    log.info("ProjectForge is now available (up and running).");
  }

  private void internalInit(ApplicationContext applicationContext,
      final boolean developmentMode)
  {
    log.info("Initializing...");
    this.applicationContext = applicationContext;
    this.cronSetup = applicationContext.getBean(CronSetup.class);
    this.myDatabaseUpdater = applicationContext.getBean(MyDatabaseUpdateService.class);
    this.userXmlPreferencesCache = applicationContext.getBean(UserXmlPreferencesCache.class);
    this.systemInfoCache = applicationContext.getBean(SystemInfoCache.class);

    Registry.getInstance().init(applicationContext);

    // Time zone
    log.info("Default TimeZone is: " + TimeZone.getDefault());
    if ("UTC".equals(TimeZone.getDefault().getID()) == false) {
      for (final String str : UTC_RECOMMENDED) {
        log.fatal(str);
      }
      for (final String str : UTC_RECOMMENDED) {
        System.err.println(str);
      }
    }
    log.info("user.timezone is: " + System.getProperty("user.timezone"));

    // Initialize Excel extensions:
    new MyXlsExportContext();

    // HIBERNATE5
    //    HibernateUtils.setConfiguration(hibernateConfiguration);

    if (DatabaseSupport.getInstance() == null) {
      DatabaseSupport.setInstance(new DatabaseSupport(HibernateUtils.getDialect()));
    }

    final UserContext internalSystemAdminUserContext = UserContext
        .__internalCreateWithSpecialUser(MyDatabaseUpdateService
            .__internalGetSystemAdminPseudoUser(), applicationContext.getBean(UserCache.class));
    final boolean missingDatabaseSchema = myDatabaseUpdater.databaseTablesWithEntriesExists();
    if (missingDatabaseSchema == true) {
      try {
        ThreadLocalUserContext.setUserContext(internalSystemAdminUserContext); // Logon admin user.
        final UpdateEntry updateEntry = DatabaseCoreInitial.getInitializationUpdateEntry(myDatabaseUpdater);
        updateEntry.runUpdate();
      } finally {
        ThreadLocalUserContext.clear();
      }
    }
    //    if (initDatabaseDao.databaseTablesWithEntriesExists() == false) {
    //      pluginAdminService.initializeActivePlugins();
    //      //    pluginsRegistry = PluginsRegistry.instance();
    //      //    pluginsRegistry.set(myDatabaseUpdater.getSystemUpdater());
    //      //    pluginsRegistry.loadPlugins(applicationContext);
    //      //    applicationContext.getBean(PluginAdminService.class).initializeActivePlugins();
    //      //    pluginsRegistry.initialize();
    //      // TODO RK 6.1 pf update mechanism.
    //      boolean runUpdates = false;
    //      if (runUpdates == true && missingDatabaseSchema == true) {
    //        try {
    //          ThreadLocalUserContext.setUserContext(internalSystemAdminUserContext); // Logon admin user.
    //          for (final AbstractPlugin plugin : pluginAdminService.getActivePlugin()) {
    //            final UpdateEntry updateEntry = plugin.getInitializationUpdateEntry();
    //            if (updateEntry != null) {
    //              updateEntry.runUpdate();
    //            }
    //          }
    //        } finally {
    //          ThreadLocalUserContext.clear();
    //        }
    //      }
    //    } else {
    //      log.info("Data-base is empty: no Plugins are loaded...");
    //    }

    SystemInfoCache.internalInitialize(systemInfoCache);

    this.initialized = true;

  }

  private void internalShutdown()
  {
    log.info("Shutdown...");
    upAndRunning = false;
    log.info("Syncing all user preferences to database.");
    userXmlPreferencesCache.forceReload();
    cronSetup.shutdown();
    try {
      final UserContext internalSystemAdminUserContext = UserContext
          .__internalCreateWithSpecialUser(MyDatabaseUpdateService
              .__internalGetSystemAdminPseudoUser(), applicationContext.getBean(UserCache.class));
      ThreadLocalUserContext.setUserContext(internalSystemAdminUserContext); // Logon admin user.
      myDatabaseUpdater.getDatabaseUpdateService().shutdownDatabase();
    } finally {
      ThreadLocalUserContext.clear();
    }
    log.info("Shutdown completed.");
  }

  /**
   * @return the startTime
   */
  public long getStartTime()
  {
    return startTime;
  }

  /**
   * @return the upAndRunning
   */
  public boolean isUpAndRunning()
  {
    return upAndRunning;
  }

  /**
   * This method should only be called in test cases!
   * 
   * @param upAndRunning the upAndRunning to set
   */
  public void internalSetUpAndRunning(final boolean upAndRunning)
  {
    log.warn("This method should only be called in test cases!");
    this.upAndRunning = upAndRunning;
  }

  private static final String[] UTC_RECOMMENDED = { //
      "**********************************************************", //
      "***                                                    ***", //
      "*** It's highly recommended to start ProjectForge      ***", //
      "*** with TimeZone UTC. This default TimeZone has to be ***", //
      "*** set before any initialization of Hibernate!!!!     ***", //
      "*** You can do this e. g. in JAVA_OPTS etc.            ***", //
      "***                                                    ***", //
      "**********************************************************" };
}
