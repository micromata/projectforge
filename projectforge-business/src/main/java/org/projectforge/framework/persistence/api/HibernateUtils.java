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

package org.projectforge.framework.persistence.api;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.HSQLDialect;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.DatabaseDialect;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO;

import de.micromata.genome.jpa.metainf.ColumnMetadata;
import de.micromata.genome.jpa.metainf.EntityMetadata;
import de.micromata.genome.util.bean.PrivateBeanUtils;

/**
 * Singleton holding the hibernate configuration. Should be configured by a servlet on initialization after hibernate
 * initialization.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class HibernateUtils
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HibernateUtils.class);

  private static final HibernateUtils instance = new HibernateUtils();

  private static boolean TEST_MODE = false;
  @Deprecated
  private Configuration configuration;

  private DatabaseDialect databaseDialect;
  // TODO Threading.
  private final Map<String, Integer> columnLengthMap = new HashMap<String, Integer>();

  /**
   * For saving performance of trying to get non Hibernate properties multiple times.
   */
  private final Set<String> columnLengthFailedSet = new HashSet<String>();

  /**
   * For internal test cases only! If true, log errors are suppressed. Please call {@link #exitTestMode()} always
   * directly after your test call!
   */
  public static void enterTestMode()
  {
    TEST_MODE = true;
    log.info("***** Entering TESTMODE.");
  }

  /**
   * For internal test cases only! If true, log errors are suppressed. Please set TEST_MODE always to false after your
   * test call!
   */
  public static void exitTestMode()
  {
    TEST_MODE = false;
    log.info("***** Exit TESTMODE.");
  }

  /**
   * Workaround for: http://opensource.atlassian.com/projects/hibernate/browse/HHH-3502:
   * 
   * @param obj
   * @return
   */
  public static Serializable getIdentifier(final BaseDO<?> obj)
  {
    if (Hibernate.isInitialized(obj) == true) {
      return ((BaseDO<?>) obj).getId();
    } else if (obj instanceof DefaultBaseDO) {
      return ((DefaultBaseDO) obj).getId();
    } else if (obj instanceof AccessEntryDO) {
      return ((AccessEntryDO) obj).getId();
    } else if (obj instanceof Kost2ArtDO) {
      return ((Kost2ArtDO) obj).getId();
    } else if (obj instanceof KundeDO) {
      return ((KundeDO) obj).getId();
    } else if (obj instanceof UserPrefEntryDO) {
      return ((UserPrefEntryDO) obj).getId();
    }

    log.error("Couldn't get the identifier of the given object (Jassist/Hibernate-Bug: HHH-3502) for class: "
        + obj.getClass().getName());
    return null;
  }

  /**
   * @param obj
   * @return
   */
  public static <T extends Serializable> void setIdentifier(final BaseDO<T> obj, final T value)
  {
    if (Hibernate.isInitialized(obj) == true) {
      obj.setId(value);
    } else if (obj instanceof DefaultBaseDO) {
      ((DefaultBaseDO) obj).setId((Integer) value);
    } else if (obj instanceof AccessEntryDO) {
      ((AccessEntryDO) obj).setId((Integer) value);
    } else if (obj instanceof Kost2ArtDO) {
      ((Kost2ArtDO) obj).setId((Integer) value);
    } else if (obj instanceof KundeDO) {
      ((KundeDO) obj).setId((Integer) value);
    } else if (obj instanceof UserPrefEntryDO) {
      ((UserPrefEntryDO) obj).setId((Integer) value);
    } else {
      log.error("Couldn't set the identifier of the given object for class: " + obj.getClass().getName());
    }
  }

  public static Serializable getIdentifier(final Object obj)
  {
    if (obj instanceof BaseDO<?>) {
      return getIdentifier((BaseDO<?>) obj);
    }
    for (final Field field : obj.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Id.class) == true && field.isAnnotationPresent(GeneratedValue.class) == true) {
        final boolean isAccessible = field.isAccessible();
        try {
          field.setAccessible(true);
          final Object idObject = field.get(obj);
          field.setAccessible(isAccessible);
          if (idObject != null && Serializable.class.isAssignableFrom(idObject.getClass()) == true) {
            return (Serializable) idObject;
          }
        } catch (final IllegalArgumentException e) {
          e.printStackTrace();
        } catch (final IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Serializable> void setIdentifier(final Object obj, final T value)
  {
    if (obj instanceof BaseDO<?>) {
      setIdentifier((BaseDO<T>) obj, value);
    }
    for (final Field field : obj.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Id.class) == true && field.isAnnotationPresent(GeneratedValue.class) == true) {
        final boolean isAccessible = field.isAccessible();
        try {
          field.setAccessible(true);
          field.set(obj, value);
          field.setAccessible(isAccessible);
        } catch (final IllegalArgumentException e) {
          e.printStackTrace();
        } catch (final IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Deprecated
  public static Configuration getConfiguration()
  {
    return instance.configuration;
  }

  public static boolean isEntity(final Class<?> entity)
  {
    return instance.internalIsEntity(entity);
  }

  public static String getDBTableName(final Class<?> entity)
  {

    EntityMetadata metadata = PfEmgrFactory.get().getMetadataRepository().getEntityMetadata(entity);
    return metadata.getDatabaseName();
  }

  /**
   * Gets the length of the given property.
   * 
   * @param entityName Class name of the entity
   * @param propertyName Java bean property name.
   * @return length if exists, otherwise null.
   */
  public static Integer getPropertyLength(final Class<?> entity, final String propertyName)
  {
    return instance.internalGetPropertyMaxLength(entity.getName(), propertyName);
  }

  /**
   * Gets the length of the given property.
   * 
   * @param entityName Class name of the entity
   * @param propertyName Java bean property name.
   * @return length if exists, otherwise null.
   */
  public static Integer getPropertyLength(final String entityName, final String propertyName)
  {
    return instance.internalGetPropertyMaxLength(entityName, propertyName);
  }

  /**
   * Shorten the length of the property if to long for the data-base. No log message is produced. The field must be
   * declared with JPA annotations in the given class (clazz). For getting and setting the property the getter and
   * setter method is used.
   * 
   * @param clazz The class where the field is declared.
   * @param object
   * @param propertyName
   * @return true If at least one property was shortened.
   */
  public static boolean shortenProperties(final Class<?> clazz, final Object object, final String... propertyNames)
  {
    boolean result = false;
    for (final String propertyName : propertyNames) {
      if (shortenProperty(clazz, object, propertyName) == true) {
        result = true;
      }
    }
    return result;
  }

  /**
   * Shorten the length of the property if to long for the data-base. No log message is produced. The field must be
   * declared with JPA annotations in the given class (clazz). For getting and setting the property the getter and
   * setter method is used.
   * 
   * @param clazz The class where the field is declared.
   * @param object
   * @param propertyName
   * @return true If the property was shortened.
   */
  public static boolean shortenProperty(final Class<?> clazz, final Object object, final String propertyName)
  {
    final Integer length = getPropertyLength(clazz, propertyName);
    if (length == null) {
      return false;
    }
    final String value = (String) BeanHelper.getProperty(object, propertyName);
    if (value != null && value.length() > length) {
      BeanHelper.setProperty(object, propertyName, value.substring(0, length - 1));
      return true;
    }
    return false;
  }

  /** Should be set at initialization of ProjectForge after initialization of hibernate. */
  public static void setConfiguration(final Configuration configuration)
  {
    instance.configuration = configuration;
  }

  public static DatabaseDialect getDialect()
  {

    if (instance.databaseDialect != null) {
      return instance.databaseDialect;
    }
    SessionFactory sessfactory = PfEmgrFactory.get().getEntityManagerFactory().unwrap(SessionFactory.class);
    Object dialect = PrivateBeanUtils.readField(sessfactory, "dialect");
    //Major class in hibernate hierachie
    if (dialect instanceof org.hibernate.dialect.PostgreSQL81Dialect) {
      instance.databaseDialect = DatabaseDialect.PostgreSQL;
    } else if (dialect instanceof HSQLDialect) {
      instance.databaseDialect = DatabaseDialect.HSQL;
    } else {
      log.warn("Unknown or unsupported dialect: " + dialect.getClass().getName());
      instance.databaseDialect = DatabaseDialect.PostgreSQL;
    }
    log.info("Using dialect: " + instance.databaseDialect.getAsString());
    return instance.databaseDialect;
  }

  private boolean internalIsEntity(final Class<?> entity)
  {
    return PfEmgrFactory.get().getMetadataRepository().isKnownEntity(entity);
  }

  private String internalGetDBTableName(final Class<?> entity)
  {
    EntityMetadata persistentClass = PfEmgrFactory.get().getMetadataRepository().findEntityMetadata(entity);
    if (persistentClass == null) {
      String msg = "Could not find persistent class for entityName '" + entity.getName()
          + "' (OK for non hibernate objects).";
      if (entity.getName().endsWith("DO") == true) {
        log.error(msg);
      } else {
        log.info(msg);
      }
      return null;
    }
    return persistentClass.getDatabaseName();
  }

  private Integer internalGetPropertyMaxLength(final String entityName, final String propertyName)
  {
    Integer length = columnLengthMap.get(getKey(entityName, propertyName));
    if (length != null) {
      return length;
    }
    if (columnLengthFailedSet.contains(getKey(entityName, propertyName)) == true) {
      return null;
    }
    EntityMetadata persistentClass = PfEmgrFactory.get().getMetadataRepository().findEntityMetadata(entityName);

    if (persistentClass == null) {
      final String msg = "Could not find persistent class for entityName '" + entityName
          + "' (OK for non hibernate objects).";
      if (entityName.endsWith("DO") == true) {
        log.error(msg);
      } else {
        log.info(msg);
      }
      putFailedEntry(entityName, propertyName);
      return null;
    }
    ColumnMetadata colinfo = persistentClass.getColumns().get(propertyName);

    if (colinfo == null) {
      log.error("Could not find column for entity '" + entityName + "' with name '" + propertyName + "'.");
      return null;
    }

    length = (int) colinfo.getMaxLength();
    columnLengthMap.put(entityName + "#" + propertyName, length);
    return length;
  }

  private void putFailedEntry(final String entityName, final String propertyName)
  {
    columnLengthFailedSet.add(getKey(entityName, propertyName));
  }

  private String getKey(final String entityName, final String propertyName)
  {
    return entityName + "#" + propertyName;
  }
}
