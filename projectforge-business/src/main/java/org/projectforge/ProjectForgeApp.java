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

package org.projectforge;

import net.fortuna.ical4j.util.CompatibilityHints;
import org.projectforge.business.multitenancy.TenantRegistry;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.business.systeminfo.SystemInfoCache;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.business.user.UserXmlPreferencesCache;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.export.MyXlsExportContext;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.database.DatabaseService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserContext;
import org.projectforge.registry.Registry;
import org.projectforge.web.WicketSupport;
import org.springframework.context.ApplicationContext;

import java.util.TimeZone;

/**
 * Doing some initialization stuff and stuff on shutdown (planned). Most stuff is yet done by WicketApplication.
 * <p>
 * TODO * DESIGNBUG enkoppeln.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

public class ProjectForgeApp
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeApp.class);

  private static ProjectForgeApp instance;

  private boolean upAndRunning;

  private final long startTime = System.currentTimeMillis();

  private ApplicationContext applicationContext;

  private DatabaseService databaseUpdater;

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
    this.databaseUpdater = applicationContext.getBean(DatabaseService.class);
    this.userXmlPreferencesCache = applicationContext.getBean(UserXmlPreferencesCache.class);
    this.systemInfoCache = applicationContext.getBean(SystemInfoCache.class);

    // Wicket workaround for not be able to proxy Kotlin base SpringBeans:
    WicketSupport.register(applicationContext);

    Registry.getInstance().init(applicationContext);

    // Time zone
    log.info("Default TimeZone is: " + TimeZone.getDefault());
    if ("UTC".equals(TimeZone.getDefault().getID()) == false) {
      for (final String str : UTC_RECOMMENDED) {
        log.error(str);
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

    SystemInfoCache.internalInitialize(systemInfoCache);

    this.initialized = true;

  }

  private TenantRegistry getTenantRegistry()
  {
    return TenantRegistryMap.getInstance().getTenantRegistry();
  }

  private UserGroupCache getUserGroupCache()
  {
    return getTenantRegistry().getUserGroupCache();
  }

  private void internalShutdown()
  {
    log.info("Shutdown...");
    upAndRunning = false;
    try {
      final UserContext internalSystemAdminUserContext = UserContext
          .__internalCreateWithSpecialUser(DatabaseService
              .__internalGetSystemAdminPseudoUser(), getUserGroupCache());
      ThreadLocalUserContext.setUserContext(internalSystemAdminUserContext); // Logon admin user.
      databaseUpdater.shutdownDatabase();
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
