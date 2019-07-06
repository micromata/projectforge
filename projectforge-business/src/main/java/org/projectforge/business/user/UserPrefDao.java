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

package org.projectforge.business.user;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.KundeDao;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskDao;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.json.*;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.framework.persistence.user.api.UserPrefParameter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.UserPrefDO;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@SuppressWarnings("deprecation")
@Repository
public class UserPrefDao extends BaseDao<UserPrefDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserPrefDao.class);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[]{"user.username", "user.firstname",
          "user.lastname"};

  @Autowired
  private Kost2Dao kost2Dao;

  @Autowired
  private KundeDao kundeDao;

  @Autowired
  private ProjektDao projektDao;

  @Autowired
  private TaskDao taskDao;

  @Autowired
  private UserDao userDao;

  @Override
  protected String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public UserPrefDao() {
    super(UserPrefDO.class);
    logDatabaseActions = false;
  }

  /**
   * Gets all names of entries of the given area for the current logged in user
   *
   * @param area
   * @return
   */
  public String[] getPrefNames(final UserPrefArea area) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    @SuppressWarnings("unchecked") final List<Object> list = getSession()
            .createQuery("select name from UserPrefDO t where user_fk=? and area = ? order by name")
            .setInteger(0, user.getId()).setParameter(1, area.getId()).list();
    final String[] result = new String[list.size()];
    int i = 0;
    for (final Object oa : list) {
      result[i++] = (String) oa;
    }
    return result;
  }

  /**
   * @param areaId
   * @return
   */
  public List<UserPrefDO> getListWithoutEntries(String areaId) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    @SuppressWarnings("unchecked") final List<Object[]> list = getSession()
            .createQuery("select id, name from UserPrefDO t where user_fk=? and area = ? order by name")
            .setInteger(0, user.getId()).setParameter(1, areaId).list();
    final List<UserPrefDO> result = new ArrayList<UserPrefDO>(list.size());
    for (final Object[] oa : list) {
      UserPrefDO userPref = new UserPrefDO();
      userPref.setUser(user);
      userPref.setArea(areaId);
      userPref.setId((Integer) oa[0]);
      userPref.setName((String) oa[1]);
      result.add(userPref);
    }
    return result;
  }


  /**
   * Does (another) entry for the given user with the given area and name already exists?
   *
   * @param id   of the current data object (null for new objects).
   * @param user
   * @param area
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  public boolean doesParameterNameAlreadyExist(final Integer id, final PFUserDO user, final UserPrefArea area,
                                               final String name) {
    Validate.notNull(user);
    Validate.notNull(area);
    return doesParameterNameAlreadyExist(id, user.getId(), area.getId(), name);
  }

  /**
   * Does (another) entry for the given user with the given area and name already exists?
   *
   * @param id     of the current data object (null for new objects).
   * @param userId
   * @param areaId
   * @param name
   * @return
   */
  @SuppressWarnings("unchecked")
  public boolean doesParameterNameAlreadyExist(final Integer id, final Integer userId, final String areaId,
                                               final String name) {
    Validate.notNull(userId);
    Validate.notNull(areaId);
    Validate.notNull(name);
    final List<UserPrefDO> list;
    if (id != null) {
      list = (List<UserPrefDO>) getHibernateTemplate().find(
              "from UserPrefDO u where id <> ? and u.user.id = ? and area = ? and name = ?",
              new Object[]{id, userId, areaId, name});
    } else {
      list = (List<UserPrefDO>) getHibernateTemplate().find(
              "from UserPrefDO u where u.user.id = ? and area = ? and name = ?",
              new Object[]{userId, areaId, name});
    }
    if (CollectionUtils.isNotEmpty(list) == true) {
      return true;
    }
    return false;
  }

  @Override
  public List<UserPrefDO> getList(final BaseSearchFilter filter) {
    final UserPrefFilter myFilter = (UserPrefFilter) filter;
    final QueryFilter queryFilter = new QueryFilter(filter);
    if (myFilter.getArea() != null) {
      queryFilter.add(Restrictions.eq("area", myFilter.getArea().getId()));
    }
    queryFilter.add(Restrictions.eq("user.id", ThreadLocalUserContext.getUserId()));
    queryFilter.addOrder(Order.asc("area"));
    queryFilter.addOrder(Order.asc("name"));
    final List<UserPrefDO> list = getList(queryFilter);
    return list;
  }

  /**
   * @param area
   * @param name
   * @return
   * @deprecated Use getUserPref(String, Integer) instead.
   */
  @Deprecated
  public UserPrefDO getUserPref(final UserPrefArea area, final String name) {
    final Integer userId = ThreadLocalUserContext.getUserId();
    return internalQuery(userId, area.getId(), name);
  }

  /**
   * @param areaId
   * @param id     id of the user pref to search.
   * @return The user pref of the areaId with the given id of the logged in user (from ThreadLocal).
   */
  public UserPrefDO getUserPref(final String areaId, final Integer id) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    @SuppressWarnings("unchecked") final List<UserPrefDO> list = (List<UserPrefDO>) getHibernateTemplate().find(
            "from UserPrefDO u where u.user.id = ? and u.area = ? and u.id = ?",
            new Object[]{user.getId(), areaId, id});
    if (list == null || list.size() != 1) {
      return null;
    }
    return list.get(0);
  }

  /**
   * Gets the single entry. If more entries found matching the user's pref with the given areaId, an NonUniqueResultException will
   * be thrown.
   * @param areaId
   * @param name
   * @return The user pref of the areaId with the given id of the logged in user (from ThreadLocal).
   */
  public UserPrefDO getUserPref(final String areaId, final String name) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    UserPrefDO userPrefDO = (UserPrefDO) getSession()
            .createQuery("from UserPrefDO t where t.user.id=:id and t.area=:area and t.name=:name")
            .setInteger("id", user.getId())
            .setParameter("area", areaId)
            .setParameter("name", name)
            .uniqueResult();
    return userPrefDO;
  }

  public List<UserPrefDO> getUserPrefs(final UserPrefArea area) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    @SuppressWarnings("unchecked") final List<UserPrefDO> list = (List<UserPrefDO>) getHibernateTemplate().find(
            "from UserPrefDO u where u.user.id = ? and u.area = ?",
            new Object[]{user.getId(), area.getId()});
    return selectUnique(list);
  }

  public List<UserPrefDO> getUserPrefs() {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    @SuppressWarnings("unchecked") final List<UserPrefDO> list = (List<UserPrefDO>) getHibernateTemplate().find(
            "from UserPrefDO u where u.user.id = ?", user.getId());
    return selectUnique(list);
  }

  /**
   * Adds the object fields as parameters to the given userPref. Fields without the annotation UserPrefParameter will be
   * ignored.
   *
   * @param userPref
   * @param obj
   * @see #fillFromUserPrefParameters(UserPrefDO, Object)
   */
  public void addUserPrefParameters(final UserPrefDO userPref, final Object obj) {
    addUserPrefParameters(userPref, obj.getClass(), obj);
  }

  /**
   * Adds the fields of the bean type represented by the given area as parameters to the given userPref. Fields without
   * the annotation UserPrefParameter will be ignored.
   *
   * @param userPref
   * @param area
   * @see #fillFromUserPrefParameters(UserPrefDO, Object)
   */
  public void addUserPrefParameters(final UserPrefDO userPref, final UserPrefArea area) {
    addUserPrefParameters(userPref, area.getBeanType(), null);
  }

  private void addUserPrefParameters(final UserPrefDO userPref, final Class<?> beanType, final Object obj) {
    Validate.notNull(userPref);
    Validate.notNull(beanType);
    final Field[] fields = beanType.getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    int no = 0;
    for (final Field field : fields) {
      if (field.isAnnotationPresent(UserPrefParameter.class) == true) {
        final UserPrefEntryDO userPrefEntry = new UserPrefEntryDO();
        userPrefEntry.setParameter(field.getName());
        if (obj != null) {
          Object value = null;
          try {
            value = field.get(obj);
            userPrefEntry.setValue(convertParameterValueToString(value));
          } catch (final IllegalAccessException ex) {
            log.error(ex.getMessage(), ex);
          }
          userPrefEntry.valueAsObject = value;
        }
        evaluateAnnotation(userPrefEntry, beanType, field);
        if (userPrefEntry.orderString == null) {
          userPrefEntry.orderString = "ZZZ" + StringHelper.format2DigitNumber(no++);
        }
        userPref.addUserPrefEntry(userPrefEntry);
      }
    }
  }

  private void evaluateAnnotations(final UserPrefDO userPref, final Class<?> beanType) {
    if (userPref.getUserPrefEntries() == null) {
      return;
    }
    final Field[] fields = beanType.getDeclaredFields();
    int no = 0;
    for (final Field field : fields) {
      if (field.isAnnotationPresent(UserPrefParameter.class) == true) {
        UserPrefEntryDO userPrefEntry = null;
        for (final UserPrefEntryDO entry : userPref.getUserPrefEntries()) {
          if (field.getName().equals(entry.getParameter()) == true) {
            userPrefEntry = entry;
            break;
          }
        }
        if (userPrefEntry == null) {
          userPrefEntry = new UserPrefEntryDO();
          evaluateAnnotation(userPrefEntry, beanType, field);
          userPref.addUserPrefEntry(userPrefEntry);
        } else {
          evaluateAnnotation(userPrefEntry, beanType, field);
        }
        if (StringUtils.isBlank(userPrefEntry.orderString) == true) {
          userPrefEntry.orderString = "ZZZ" + StringHelper.format2DigitNumber(no++);
        }
        userPrefEntry.setParameter(field.getName());
      }
    }
  }

  private void evaluateAnnotation(final UserPrefEntryDO userPrefEntry, final Class<?> beanType, final Field field) {
    final UserPrefParameter ann = field.getAnnotation(UserPrefParameter.class);
    userPrefEntry.i18nKey = ann.i18nKey();
    userPrefEntry.tooltipI18nKey = ann.tooltipI18nKey();
    userPrefEntry.dependsOn = StringUtils.isNotBlank(ann.dependsOn()) ? ann.dependsOn() : null;
    userPrefEntry.required = ann.required();
    userPrefEntry.multiline = ann.multiline();
    userPrefEntry.orderString = StringUtils.isNotBlank(ann.orderString()) ? ann.orderString() : null;
    if (String.class.isAssignableFrom(field.getType()) == true) {
      userPrefEntry.maxLength = HibernateUtils.getPropertyLength(beanType, field.getName());
    }
    userPrefEntry.type = field.getType();
  }

  /**
   * Fill object fields from the parameters of the given userPref.
   *
   * @param userPref
   * @param obj
   * @see #addUserPrefParameters(UserPrefDO, Object)
   */
  public void fillFromUserPrefParameters(final UserPrefDO userPref, final Object obj) {
    fillFromUserPrefParameters(userPref, obj, false);
  }

  /**
   * Fill object fields from the parameters of the given userPref.
   *
   * @param userPref
   * @param obj
   * @param preserveExistingValues If true then existing value will not be overwritten by the user pref object. Default
   *                               is false.
   * @see #addUserPrefParameters(UserPrefDO, Object)
   */
  public void fillFromUserPrefParameters(final UserPrefDO userPref, final Object obj,
                                         final boolean preserveExistingValues) {
    Validate.notNull(userPref);
    Validate.notNull(obj);
    final Field[] fields = obj.getClass().getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    if (userPref.getUserPrefEntries() != null) {
      for (final UserPrefEntryDO entry : userPref.getUserPrefEntries()) {
        Field field = null;
        for (final Field f : fields) {
          if (f.getName().equals(entry.getParameter()) == true) {
            field = f;
            break;
          }
        }
        if (field == null) {
          log.error("Declared field '" + entry.getParameter() + "' not found for " + obj.getClass()
                  + ". Ignoring parameter.");
        } else {
          final Object value = getParameterValue(field.getType(), entry.getValue());
          try {
            if (preserveExistingValues == true) {
              final Object oldValue = field.get(obj);
              if (oldValue != null) {
                if (oldValue instanceof String) {
                  if (((String) oldValue).length() > 0) {
                    // Preserve existing value:
                    continue;
                  }
                } else {
                  // Preserve existing value:
                  continue;
                }
              }
            }
            field.set(obj, value);
          } catch (final IllegalArgumentException ex) {
            log.error(ex.getMessage()
                    + " While setting declared field '"
                    + entry.getParameter()
                    + "' of "
                    + obj.getClass()
                    + ". Ignoring parameter.", ex);
          } catch (final IllegalAccessException ex) {
            log.error(ex.getMessage()
                    + " While setting declared field '"
                    + entry.getParameter()
                    + "' of "
                    + obj.getClass()
                    + ". Ignoring parameter.", ex);
          }
        }
      }
    }
  }

  public void setValueObject(final UserPrefEntryDO userPrefEntry, final Object value) {
    userPrefEntry.setValue(convertParameterValueToString(value));
    updateParameterValueObject(userPrefEntry);
  }

  public String convertParameterValueToString(final Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BaseDO<?>) {
      return String.valueOf(((BaseDO<?>) value).getId());
    }
    return String.valueOf(value);
  }

  /**
   * Sets the value object by converting it from the value string. The type of the userPrefEntry must be given.
   *
   * @param userPrefEntry
   */
  public void updateParameterValueObject(final UserPrefEntryDO userPrefEntry) {
    userPrefEntry.valueAsObject = getParameterValue(userPrefEntry.getType(), userPrefEntry.getValue());
  }

  /**
   * @see #convertParameterValueToString(Object)
   */
  @SuppressWarnings("unchecked")
  public Object getParameterValue(final Class<?> type, final String str) {
    if (str == null) {
      return null;
    }
    if (type.isAssignableFrom(String.class) == true) {
      return str;
    } else if (type.isAssignableFrom(Integer.class) == true) {
      return Integer.valueOf(str);
    } else if (DefaultBaseDO.class.isAssignableFrom(type) == true) {
      final Integer id = NumberHelper.parseInteger(str);
      if (id != null) {
        if (PFUserDO.class.isAssignableFrom(type) == true) {
          return userDao.getOrLoad(id);
        } else if (TaskDO.class.isAssignableFrom(type) == true) {
          return taskDao.getOrLoad(id);
        } else if (Kost2DO.class.isAssignableFrom(type) == true) {
          return kost2Dao.getOrLoad(id);
        } else if (ProjektDO.class.isAssignableFrom(type) == true) {
          return projektDao.getOrLoad(id);
        } else {
          log.warn("getParameterValue: Type '" + type + "' not supported. May-be it does not work.");
          return getHibernateTemplate().load(type, id);
        }
      } else {
        return null;
      }
    } else if (KundeDO.class.isAssignableFrom(type) == true) {
      final Integer id = NumberHelper.parseInteger(str);
      if (id != null) {
        return kundeDao.getOrLoad(id);
      } else {
        return null;
      }
    } else if (type.isEnum() == true) {
      return Enum.valueOf((Class<Enum>) type, str);
    }
    log.error("UserPrefDao does not yet support parameters from type: " + type);
    return null;
  }

  @Override
  public UserPrefDO internalGetById(final Serializable id) {
    final UserPrefDO userPref = super.internalGetById(id);
    if (userPref == null) {
      return null;
    }
    if (userPref.getAreaObject() != null) {
      evaluateAnnotations(userPref, userPref.getAreaObject().getBeanType());
    }
    return userPref;
  }

  /**
   * @return Always true, no generic select access needed for user pref objects.
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final UserPrefDO obj, final UserPrefDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    if (accessChecker.userEquals(user, obj.getUser()) == true) {
      return true;
    }
    if (throwException == true) {
      throw new AccessException("userPref.error.userIsNotOwner");
    } else {
      return false;
    }
  }

  @Override
  public UserPrefDO newInstance() {
    return new UserPrefDO();
  }

  public Object deserizalizeValueObject(UserPrefDO userPref) {
    if (userPref.getValueType() == null)
      return null;
    userPref.setValueObject(fromJson(userPref.getValueString(), userPref.getValueType()));
    return userPref.getValueObject();
  }

  @Override
  protected void onSaveOrModify(UserPrefDO obj) {
    if (obj.getValueObject() == null) {
      obj.setValueString(null);
      obj.setValueTypeString(null);
    } else {
      obj.setValueString(toJson(obj.getValueObject()));
      obj.setValueTypeString(obj.getValueObject().getClass().getName());
    }
  }

  /**
   * Without check access.
   *
   * @param userId Must be given.
   * @param area   Must be not blank.
   * @param name   Optional, may-be null.
   */
  public UserPrefDO internalQuery(Integer userId, String area, String name) {
    Validate.notNull(userId);
    Validate.notBlank(area);
    // Try to find any existing entry:
    final String queryBaseString = "from UserPrefDO t where user_fk=:userId and area=:area and name";
    final String queryString;
    if (name == null) {
      queryString = queryBaseString + " is null";
    } else {
      queryString = queryBaseString + "=:name";
    }
    final Query query = getSession()
            .createQuery(queryString)
            .setInteger("userId", userId)
            .setParameter("area", area);
    if (name != null) {
      query.setParameter("name", name);
    }
    return (UserPrefDO) query.uniqueResult();
  }

  /**
   * Checks if the user pref already exists in the data base by querying the data base with user id, area and name.
   * The id of the given obj is ignored.
   */
  @Override
  public Serializable internalSaveOrUpdate(UserPrefDO obj) {
    Validate.notNull(obj.getUser());
    synchronized (this) { // Avoid parallel insert, update, delete operations.
      final UserPrefDO dbUserPref = (UserPrefDO) internalQuery(obj.getUser().getId(), obj.getArea(), obj.getName());
      if (dbUserPref == null) {
        obj.setId(null); // Add new entry (ignore id of any previous existing entry).
        return super.internalSaveOrUpdate(obj);
      } else {
        obj.setId(dbUserPref.getId());
        dbUserPref.setValueObject(obj.getValueObject());
        if (dbUserPref.getUserPrefEntries() != null ||
                obj.getUserPrefEntries() != null) {
          // Legacy entries:
          if (CollectionUtils.isEmpty(obj.getUserPrefEntries())) {
            // All existing entries are deleted, so clear db entries:
            dbUserPref.getUserPrefEntries().clear();
          } else {
            // New entries exists, so we've to add them:
            if (dbUserPref.getUserPrefEntries() == null) {
              dbUserPref.setUserPrefEntries(new HashSet<>());
            } else {
              // Remove entries in db not existing anymore in given obj:
              for (Iterator<UserPrefEntryDO> it = dbUserPref.getUserPrefEntries().iterator(); it.hasNext(); ) {
                UserPrefEntryDO entry = it.next();
                if (obj.getUserPrefEntry(entry.getParameter()) == null) {
                  // This entry was removed (it's not present in the given object anymore:
                  it.remove();
                }
              }
            }
            // Now we've to add / update all entries in the db of given obj:
            for (UserPrefEntryDO newEntry : obj.getUserPrefEntries()) {
              UserPrefEntryDO dbEntry = dbUserPref.getUserPrefEntry(newEntry.getParameter());
              if (dbEntry == null) {
                // New entry:
                dbUserPref.getUserPrefEntries().add(newEntry);
                newEntry.setId(null);
              } else {
                // Update current entry:
                dbEntry.copyValuesFrom(newEntry, "id");
              }
            }
          }
        }
        super.internalUpdate(dbUserPref);
        obj.setId(dbUserPref.getId());
        return obj.getId();
      }
    }
  }

  /**
   * Only for synchronization with {@link #internalSaveOrUpdate(UserPrefDO)}.
   *
   * @param obj
   * @throws AccessException
   */
  @Override
  public void delete(UserPrefDO obj) throws AccessException {
    synchronized (this) {
      super.delete(obj);
    }
  }

  private static final String MAGIC_JSON_START = "^JSON:";

  private String toJson(Object obj) {
    try {
      return MAGIC_JSON_START + createObjectMapper().writeValueAsString(obj);
    } catch (JsonProcessingException ex) {
      log.error("Error while trying to serialze object as json: " + ex.getMessage(), ex);
      return "";
    }
  }

  private boolean isJsonObject(String value) {
    return StringUtils.startsWith(value, MAGIC_JSON_START);
  }

  private <T> T fromJson(String json, final Class<T> classOfT) {
    if (!isJsonObject(json))
      return null;
    json = json.substring(MAGIC_JSON_START.length());
    try {
      return createObjectMapper().readValue(json, classOfT);
    } catch (IOException ex) {
      log.error("Can't deserialize json object: " + ex.getMessage() + " json=" + json, ex);
      return null;
    }
  }

  public static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
    mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    SimpleModule module = new SimpleModule();
    module.addSerializer(LocalDate.class, new LocalDateSerializer());
    module.addDeserializer(LocalDate.class, new LocalDateDeserializer());

    module.addSerializer(PFDateTime.class, new PFDateTimeSerializer());
    module.addDeserializer(PFDateTime.class, new PFDateTimeDeserializer());

    module.addSerializer(java.util.Date.class, new UtilDateSerializer(UtilDateFormat.ISO_DATE_TIME_SECONDS));
    module.addDeserializer(java.util.Date.class, new UtilDateDeserializer());

    module.addSerializer(Timestamp.class, new TimestampSerializer(UtilDateFormat.ISO_DATE_TIME_MILLIS));
    module.addDeserializer(Timestamp.class, new TimestampDeserializer());

    module.addSerializer(java.sql.Date.class, new SqlDateSerializer());
    module.addDeserializer(java.sql.Date.class, new SqlDateDeserializer());

    mapper.registerModule(module);
    mapper.registerModule(new KotlinModule());
    return mapper;
  }
}
