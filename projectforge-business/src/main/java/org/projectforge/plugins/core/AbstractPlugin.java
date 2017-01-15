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

package org.projectforge.plugins.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.projectforge.business.user.UserPrefAreaRegistry;
import org.projectforge.business.user.UserRight;
import org.projectforge.business.user.UserXmlPreferencesBaseDOSingleValueConverter;
import org.projectforge.business.user.UserXmlPreferencesDao;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.database.DatabaseUpdateService;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter;
import org.projectforge.registry.Registry;
import org.projectforge.registry.RegistryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public abstract class AbstractPlugin
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AbstractPlugin.class);

  @Autowired
  protected ApplicationContext applicationContext;

  @Autowired
  protected DatabaseUpdateService myDatabaseUpdater;

  @Autowired
  protected UserXmlPreferencesDao userXmlPreferencesDao;

  @Autowired
  protected AccessChecker accessChecker;

  @Autowired
  UserRightService userRights;

  @Deprecated
  private String resourceBundleName;

  private List<String> resourceBundleNames = new ArrayList<>();

  private boolean initialized;

  @Deprecated
  private static Set<Class<?>> initializedPlugins = new HashSet<Class<?>>();

  public String getResourceBundleName()
  {
    return resourceBundleName;
  }

  public final void init()
  {
    synchronized (initializedPlugins) {
      if (initializedPlugins.contains(this.getClass()) == true || initialized == true) {
        log.warn("Ignoring multiple initialization of plugin.");
        return;
      }
      initialized = true;
      initializedPlugins.add(this.getClass());
      log.info("Initializing plugin: " + getClass());
      initialize();
    }
  }

  /**
   * Is called on initialization of the plugin by the method {@link #init()}.
   */
  protected abstract void initialize();

  /**
   * @return the initialized
   */
  public boolean isInitialized()
  {
    return initialized;
  }

  public List<String> getResourceBundleNames()
  {
    return resourceBundleNames;
  }

  /**
   * @param resourceBundleName
   * @return this for chaining.
   */
  protected AbstractPlugin addResourceBundle(final String resourceBundleName)
  {
    resourceBundleNames.add(resourceBundleName);
    return this;
  }

  /**
   * @param id           The unique plugin id.
   * @param daoClassType The dao object type.
   * @param baseDao      The dao itself.
   * @param i18nPrefix   The prefix for i18n keys.
   * @return New RegistryEntry.
   */
  protected RegistryEntry register(final String id, final Class<? extends BaseDao<?>> daoClassType,
      final BaseDao<?> baseDao,
      final String i18nPrefix)
  {
    if (baseDao == null) {
      throw new IllegalArgumentException(
          id
              + ": Dao object is null. May-be the developer forgots to initialize it in pluginContext.xml or the setter method is not given in the main plugin class!");
    }
    final RegistryEntry entry = new RegistryEntry(id, daoClassType, baseDao, i18nPrefix);
    register(entry);
    return entry;
  }

  /**
   * Registers the given entry.
   *
   * @param entry
   * @return The registered registry entry for chaining.
   * @see Registry#register(RegistryEntry)
   */
  protected RegistryEntry register(final RegistryEntry entry)
  {
    Validate.notNull(entry);
    Registry.getInstance().register(entry);
    return entry;
  }

  /**
   * Registers a right which is responsible for the access management.
   *
   * @param right
   * @return this for chaining.
   */
  protected AbstractPlugin registerRight(final UserRight right)
  {
    userRights.addRight(right);
    return this;
  }

  /**
   * Registers a new user preferences areas (shown in the list of 'own settings' of each user).
   *
   * @param areaId
   * @param cls
   * @param i18nSuffix
   * @return Created and registered UserPrefArea.
   * @see UserPrefArea#UserPrefArea(String, Class, String)
   */
  protected UserPrefArea registerUserPrefArea(final String areaId, final Class<?> cls, final String i18nSuffix)
  {
    final UserPrefArea userPrefArea = new UserPrefArea(areaId, cls, i18nSuffix);
    UserPrefAreaRegistry.instance().register(userPrefArea);
    return userPrefArea;
  }

  /**
   * The annotations of the given classes will be processed by xstream which is used for marshalling and unmarshalling
   * user xml preferences.
   *
   * @param classes
   * @return this for chaining.
   * @see UserXmlPreferencesDao#processAnnotations(Class...)
   */
  protected AbstractPlugin processUserXmlPreferencesAnnotations(final Class<?>... classes)
  {
    userXmlPreferencesDao.processAnnotations(classes);
    return this;
  }

  /**
   * Register converters before marshaling and unmarshaling by XStream.
   *
   * @param daoClass Class of the dao.
   * @param doClass  Class of the DO which will be converted.
   * @see UserXmlPreferencesBaseDOSingleValueConverter#UserXmlPreferencesBaseDOSingleValueConverter(Class, Class)
   */
  public void registerUserXmlPreferencesConverter(final Class<? extends BaseDao<?>> daoClass,
      final Class<? extends BaseDO<?>> doClass)
  {
    userXmlPreferencesDao.registerConverter(daoClass, doClass, 10);
  }

  /**
   * Register converters before marshaling and unmarshaling by XStream.
   *
   * @param daoClass Class of the dao.
   * @param doClass  Class of the DO which will be converted.
   * @param priority The priority needed by xtream for using converters in the demanded order.
   * @see UserXmlPreferencesBaseDOSingleValueConverter#UserXmlPreferencesBaseDOSingleValueConverter(Class, Class)
   */
  public void registerUserXmlPreferencesConverter(final Class<? extends BaseDao<?>> daoClass,
      final Class<? extends BaseDO<?>> doClass, final int priority)
  {
    userXmlPreferencesDao.registerConverter(daoClass, doClass, priority);
  }

  /**
   * Override this method if an update entry for initialization does exist. This will be called, if the plugin runs the
   * first time.
   *
   * @return null at default.
   * @see ToDoPlugin
   */
  public UpdateEntry getInitializationUpdateEntry()
  {
    return null;
  }

  /**
   * Override this method if update entries does exist for this plugin.
   *
   * @return null at default.
   */
  public List<UpdateEntry> getUpdateEntries()
  {
    return null;
  }

  @Deprecated
  public void onBeforeRestore(final XStreamSavingConverter xstreamSavingConverter, final Object obj)
  {
  }

  @Deprecated
  public void onAfterRestore(final XStreamSavingConverter xstreamSavingConverter, final Object obj,
      final Serializable newId)
  {
  }
}
