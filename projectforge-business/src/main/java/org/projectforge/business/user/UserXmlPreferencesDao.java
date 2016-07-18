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

package org.projectforge.business.user;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.refactoring.RefactoringService;
import org.projectforge.business.scripting.xstream.RecentScriptCalls;
import org.projectforge.business.scripting.xstream.ScriptCallData;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskFilter;
import org.projectforge.business.timesheet.TimesheetPrefData;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.GZIPHelper;
import org.projectforge.framework.xstream.XStreamHelper;
import org.projectforge.framework.xstream.converter.JodaDateMidnightConverter;
import org.projectforge.framework.xstream.converter.JodaDateTimeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.thoughtworks.xstream.XStream;

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database as
 * xml (compressed (gzip and base64) for larger xml content).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Repository
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class UserXmlPreferencesDao
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserXmlPreferencesDao.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private UserDao userDao;

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private RefactoringService refService;

  @Autowired
  private TenantDao tenantDao;

  private final XStream xstream = XStreamHelper.createXStream();

  @PostConstruct
  private void init()
  {
    xstream.processAnnotations(new Class<?>[] { UserXmlPreferencesMap.class, TaskFilter.class, TimesheetPrefData.class,
        ScriptCallData.class, RecentScriptCalls.class });
    registerConverter(UserDao.class, PFUserDO.class, 20);
    registerConverter(GroupDao.class, GroupDO.class, 19);
    registerConverter(TaskDao.class, TaskDO.class, 18);
    xstream.registerConverter(new JodaDateTimeConverter());
    xstream.registerConverter(new JodaDateMidnightConverter());
  }

  /**
   * Process the given classes before marshaling and unmarshaling by XStream. This method is usable by plugins.
   * 
   * @param classes
   */
  public void processAnnotations(final Class<?>... classes)
  {
    xstream.processAnnotations(classes);
  }

  /**
   * Register converters before marshaling and unmarshaling by XStream. This method is usable by plugins.
   * 
   * @param daoClass Class of the dao.
   * @param doClass Class of the DO which will be converted.
   * @param priority The priority needed by xtream for using converters in the demanded order.
   * @see UserXmlPreferencesBaseDOSingleValueConverter#UserXmlPreferencesBaseDOSingleValueConverter(Class, Class)
   */
  public void registerConverter(final Class<? extends BaseDao<?>> daoClass, final Class<? extends BaseDO<?>> doClass,
      final int priority)
  {
    xstream.registerConverter(new UserXmlPreferencesBaseDOSingleValueConverter(applicationContext, daoClass, doClass),
        priority);
  }

  /**
   * Throws AccessException if the context user is not admin user and not owner of the UserXmlPreferences, meaning the
   * given userId must be the id of the context user.
   * 
   * @param userId
   */
  private UserXmlPreferencesDO getUserPreferencesByUserId(final Integer userId, final String key,
      final boolean checkAccess)
  {
    if (checkAccess == true) {
      checkAccess(userId);
    }
    final List<UserXmlPreferencesDO> list = PfEmgrFactory.get().runInTrans((emgr) -> {
      return emgr.select(UserXmlPreferencesDO.class,
          "select u from UserXmlPreferencesDO u where u.user.id = :userid and u.key = :key",
          "userid", userId, "key", key);
    });
    Validate.isTrue(list.size() <= 1);
    if (list.size() == 1) {
      return list.get(0);
    } else
      return null;
  }

  /**
   * Throws AccessException if the context user is not admin user and not owner of the UserXmlPreferences, meaning the
   * given userId must be the id of the context user.
   * 
   * @param userId
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<UserXmlPreferencesDO> getUserPreferencesByUserId(final Integer userId)
  {
    checkAccess(userId);
    return PfEmgrFactory.get().runInTrans((emgr) -> {
      return emgr.select(UserXmlPreferencesDO.class, "select u from UserXmlPreferencesDO u where u.user.id = :userid",
          "userid", userId);
    });
  }

  /**
   * Checks if the given userIs is equals to the context user or the if the user is an admin user. If not a
   * AccessException will be thrown.
   * 
   * @param userId
   */
  public void checkAccess(final Integer userId)
  {
    Validate.notNull(userId);
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (ObjectUtils.equals(userId, user.getId()) == false) {
      accessChecker.checkIsLoggedInUserMemberOfAdminGroup();
    }
  }

  /**
   * Here you can update user preferences formats by manipulation the stored xml string.
   * 
   * @param userId
   * 
   * @param userPrefs
   * @param logError
   */
  public Object deserialize(Integer userId, final UserXmlPreferencesDO userPrefs, final boolean logError)
  {
    String xml = null;
    try {
      UserXmlPreferencesMigrationDao.migrate(userPrefs);
      xml = userPrefs.getSerializedSettings();
      if (xml == null || xml.length() == 0) {
        return null;
      }
      if (xml.startsWith("!") == true) {
        // Uncompress value:
        final String uncompressed = GZIPHelper.uncompress(xml.substring(1));
        xml = uncompressed;
      }
      String sourceClassName = getSourceClassName(xml);
      String oldPackageName = null;
      String newPackageName = null;
      if (sourceClassName != null) {
        oldPackageName = refService.getPackageName(sourceClassName);
        newPackageName = refService.getNewPackageNameForFullQualifiedClass(sourceClassName);
      }
      Object value = null;
      if (log.isDebugEnabled()) {
        log.debug("UserId: " + userId + " Object to deserialize: " + xml);
      }
      if (sourceClassName != null && oldPackageName != null && newPackageName != null) {
        value = XStreamHelper.fromXml(xstream, xml, oldPackageName, newPackageName);
      } else {
        value = XStreamHelper.fromXml(xstream, xml);
      }
      return value;
    } catch (final Throwable ex) {
      if (logError == true) {
        log.warn("Can't deserialize user preferences: "
            + ex.getMessage()
            + " for user: "
            + userPrefs.getUserId()
            + ":"
            + userPrefs.getKey()
            + " (may-be ok after a new ProjectForge release). xml="
            + xml);
      }
      return null;
    }
  }

  private String getSourceClassName(String xml)
  {
    String[] elements = xml.split("\n");
    if (elements.length > 0) {
      String result = elements[0].replace("<", "").replace(">", "");
      if (StringUtils.countMatches(result, ".") > 1) {
        return result;
      }
    }
    return null;
  }

  public String serialize(final UserXmlPreferencesDO userPrefs, final Object value)
  {
    final String xml = XStreamHelper.toXml(xstream, value);

    if (xml.length() > 1000) {
      // Compress value:
      final String compressed = GZIPHelper.compress(xml);
      userPrefs.setSerializedSettings("!" + compressed);
    } else {
      userPrefs.setSerializedSettings(xml);
    }
    return xml;
  }

  // REQUIRES_NEW needed for avoiding a lot of new data base connections from HibernateFilter.
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public void saveOrUpdateUserEntries(final Integer userId, final UserXmlPreferencesMap data, final boolean checkAccess)
  {
    for (final Map.Entry<String, Object> prefEntry : data.getPersistentData().entrySet()) {
      final String key = prefEntry.getKey();
      if (data.isModified(key) == true) {
        try {
          saveOrUpdate(userId, key, prefEntry.getValue(), checkAccess);
        } catch (final Throwable ex) {
          log.warn(ex.getMessage(), ex);
        }
        data.setModified(key, false);
      }
    }
  }

  /**
   * @param sheet
   * @param userId If null, then user will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setUser(final UserXmlPreferencesDO userPrefs, final Integer userId)
  {
    final PFUserDO user = userDao.getOrLoad(userId);
    userPrefs.setUser(user);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
  public void saveOrUpdate(final Integer userId, final String key, final Object entry, final boolean checkAccess)
  {
    if (accessChecker.isDemoUser(userId) == true) {
      // Do nothing.
      return;
    }
    boolean isNew = false;
    UserXmlPreferencesDO userPrefs = getUserPreferencesByUserId(userId, key, checkAccess);
    final Date date = new Date();
    if (userPrefs == null) {
      isNew = true;
      userPrefs = new UserXmlPreferencesDO();
      userPrefs.setTenant(tenantDao.getDefaultTenant());
      userPrefs.setCreated(date);
      userPrefs.setUser(userDao.internalGetById(userId));
      userPrefs.setKey(key);
    }
    final String xml = serialize(userPrefs, entry);
    if (log.isDebugEnabled()) {
      log.debug("UserXmlPrefs serialize to db: " + xml);
    }
    userPrefs.setLastUpdate(date);
    userPrefs.setVersion();
    if (isNew == true) {
      if (log.isDebugEnabled() == true) {
        log.debug("Storing new user preference for user '" + userId + "': " + xml);
      }
      hibernateTemplate.save(userPrefs);
    } else {
      if (log.isDebugEnabled() == true) {
        log.debug("Updating user preference for user '" + userPrefs.getUserId() + "': " + xml);
      }
      hibernateTemplate.update(userPrefs);
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void remove(final Integer userId, final String key)
  {
    if (accessChecker.isDemoUser(userId) == true) {
      // Do nothing.
      return;
    }
    final UserXmlPreferencesDO userPreferencesDO = getUserPreferencesByUserId(userId, key, true);
    if (userPreferencesDO != null) {
      hibernateTemplate.delete(userPreferencesDO);
    }
  }
}
