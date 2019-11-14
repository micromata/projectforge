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
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.projectforge.framework.time.PFDateTime;
import org.projectforge.framework.utils.NumberHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
  public String[] getAdditionalSearchFields() {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  public UserPrefDao() {
    super(UserPrefDO.class);
    logDatabaseActions = false;
  }

  /**
   * Gets all names of entries of the given area for the current logged in user
   */
  public String[] getPrefNames(final UserPrefArea area) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    List<String> names = em.createNamedQuery(UserPrefDO.FIND_NAMES_BY_USER_AND_AREA, String.class)
            .setParameter("userId", user.getId())
            .setParameter("area", area.getId())
            .getResultList();
    final String[] result = new String[names.size()];
    int i = 0;
    for (final Object oa : names) {
      result[i++] = (String) oa;
    }
    return result;
  }

  public List<UserPrefDO> getListWithoutEntries(String areaId) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final List<Object[]> list = em
            .createNamedQuery(UserPrefDO.FIND_IDS_AND_NAMES_BY_USER_AND_AREA, Object[].class)
            .setParameter("userId", user.getId())
            .setParameter("area", areaId)
            .getResultList();
    final List<UserPrefDO> result = new ArrayList<>(list.size());
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
   * @param id of the current data object (null for new objects).
   */
  public boolean doesParameterNameAlreadyExist(final Integer id, final PFUserDO user, final UserPrefArea area,
                                               final String name) {
    Validate.notNull(user);
    Validate.notNull(area);
    return doesParameterNameAlreadyExist(id, user.getId(), area.getId(), name);
  }

  /**
   * Does (another) entry for the given user with the given area and name already exists?
   *
   * @param id of the current data object (null for new objects).
   */
  public boolean doesParameterNameAlreadyExist(final Integer id, final Integer userId, final String areaId,
                                               final String name) {
    Validate.notNull(userId);
    Validate.notNull(areaId);
    Validate.notNull(name);
    final UserPrefDO userPref;
    if (id != null) {
      userPref = SQLHelper.ensureUniqueResult(em.createNamedQuery(UserPrefDO.FIND_OTHER_BY_USER_AND_AREA_AND_NAME, UserPrefDO.class)
              .setParameter("id", id)
              .setParameter("userId", userId)
              .setParameter("area", areaId)
              .setParameter("name", name));
    } else {
      userPref = SQLHelper.ensureUniqueResult(em.createNamedQuery(UserPrefDO.FIND_BY_USER_AND_AREA_AND_NAME, UserPrefDO.class)
              .setParameter("userId", userId)
              .setParameter("area", areaId)
              .setParameter("name", name));
    }
    return userPref != null;
  }

  @Override
  public List<UserPrefDO> getList(final BaseSearchFilter filter) {
    final UserPrefFilter myFilter = (UserPrefFilter) filter;
    final QueryFilter queryFilter = new QueryFilter(filter);
    if (myFilter.getArea() != null) {
      queryFilter.add(QueryFilter.eq("area", myFilter.getArea().getId()));
    }
    queryFilter.add(QueryFilter.eq("user.id", ThreadLocalUserContext.getUserId()));
    queryFilter.addOrder(SortProperty.asc("area"));
    queryFilter.addOrder(SortProperty.asc("name"));
    return getList(queryFilter);
  }

  /**
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
    return getUserPref(user.getId(), areaId, id);
  }

  private UserPrefDO getUserPref(final Integer userId, final String areaId, final Integer id) {
    return SQLHelper.ensureUniqueResult(
            em.createNamedQuery(UserPrefDO.FIND_BY_USER_AND_AREA_AND_ID, UserPrefDO.class)
                    .setParameter("userId", userId)
                    .setParameter("area", areaId)
                    .setParameter("id", id));
  }

  public List<UserPrefDO> getUserPrefs(final UserPrefArea area) {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final List<UserPrefDO> list = em.createNamedQuery(UserPrefDO.FIND_BY_USER_ID_AND_AREA, UserPrefDO.class)
            .setParameter("userId", user.getId())
            .setParameter("area", area.getId())
            .getResultList();
    return selectUnique(list);
  }

  public List<UserPrefDO> getUserPrefs() {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    final List<UserPrefDO> list = em.createNamedQuery(UserPrefDO.FIND_BY_USER_ID, UserPrefDO.class)
            .setParameter("userId", user.getId())
            .getResultList();
    return selectUnique(list);
  }

  /**
   * Adds the object fields as parameters to the given userPref. Fields without the annotation UserPrefParameter will be
   * ignored.
   *
   * @see #fillFromUserPrefParameters(UserPrefDO, Object)
   */
  public void addUserPrefParameters(final UserPrefDO userPref, final Object obj) {
    addUserPrefParameters(userPref, obj.getClass(), obj);
  }

  /**
   * Adds the fields of the bean type represented by the given area as parameters to the given userPref. Fields without
   * the annotation UserPrefParameter will be ignored.
   *
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
      if (field.isAnnotationPresent(UserPrefParameter.class)) {
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
      if (field.isAnnotationPresent(UserPrefParameter.class)) {
        UserPrefEntryDO userPrefEntry = null;
        for (final UserPrefEntryDO entry : userPref.getUserPrefEntries()) {
          if (field.getName().equals(entry.getParameter())) {
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
        if (StringUtils.isBlank(userPrefEntry.orderString)) {
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
    if (String.class.isAssignableFrom(field.getType())) {
      userPrefEntry.maxLength = HibernateUtils.getPropertyLength(beanType, field.getName());
    }
    userPrefEntry.type = field.getType();
  }

  /**
   * Fill object fields from the parameters of the given userPref.
   *
   * @see #addUserPrefParameters(UserPrefDO, Object)
   */
  public void fillFromUserPrefParameters(final UserPrefDO userPref, final Object obj) {
    fillFromUserPrefParameters(userPref, obj, false);
  }

  /**
   * Fill object fields from the parameters of the given userPref.
   *
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
          if (f.getName().equals(entry.getParameter())) {
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
            if (preserveExistingValues) {
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
          } catch (final IllegalArgumentException | IllegalAccessException ex) {
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
    if (type.isAssignableFrom(String.class)) {
      return str;
    } else if (type.isAssignableFrom(Integer.class)) {
      return Integer.valueOf(str);
    } else if (DefaultBaseDO.class.isAssignableFrom(type)) {
      final Integer id = NumberHelper.parseInteger(str);
      if (id != null) {
        if (PFUserDO.class.isAssignableFrom(type)) {
          return userDao.getOrLoad(id);
        } else if (TaskDO.class.isAssignableFrom(type)) {
          return taskDao.getOrLoad(id);
        } else if (Kost2DO.class.isAssignableFrom(type)) {
          return kost2Dao.getOrLoad(id);
        } else if (ProjektDO.class.isAssignableFrom(type)) {
          return projektDao.getOrLoad(id);
        } else {
          log.warn("getParameterValue: Type '" + type + "' not supported. May-be it does not work.");
          return em.getReference(type, id);
        }
      } else {
        return null;
      }
    } else if (KundeDO.class.isAssignableFrom(type)) {
      final Integer id = NumberHelper.parseInteger(str);
      if (id != null) {
        return kundeDao.getOrLoad(id);
      } else {
        return null;
      }
    } else if (type.isEnum()) {
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
  public boolean hasUserSelectAccess(final PFUserDO user, final boolean throwException) {
    return true;
  }

  @Override
  public boolean hasAccess(final PFUserDO user, final UserPrefDO obj, final UserPrefDO oldObj,
                           final OperationType operationType,
                           final boolean throwException) {
    if (accessChecker.userEquals(user, obj.getUser())) {
      return true;
    }
    if (throwException) {
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
    if (name == null) {
      return SQLHelper.ensureUniqueResult(
              em.createNamedQuery(UserPrefDO.FIND_BY_USER_ID_AND_AREA_AND_NULLNAME, UserPrefDO.class)
                      .setParameter("userId", userId)
                      .setParameter("area", area));
    } else {
      return SQLHelper.ensureUniqueResult(
              em.createNamedQuery(UserPrefDO.FIND_BY_USER_AND_AREA_AND_NAME, UserPrefDO.class)
                      .setParameter("userId", userId)
                      .setParameter("area", area)
                      .setParameter("name", name));
    }
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
      log.error("Can't deserialize json object (may-be incompatible ProjectForge versions): " + ex.getMessage() + " json=" + json, ex);
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
