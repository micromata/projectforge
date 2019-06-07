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

package de.micromata.hibernate.history.delta;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.SessionFactory;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.CollectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy used for XML persistence of DB.
 * 
 * @author Wolfgang Jung (w.jung@micromata.de)
 * 
 */
public class DeltaSetCalculator
{
  private static final Logger log = LoggerFactory.getLogger(DeltaSetCalculator.class);

  private DeltaSetCalculator()
  {
    // do nothing
  }

  /*
   * A hibernate-specific calculation. Uses the values passed to the Hibernate Interceptor.onFlushDirty() to perform the
   * calculation.
   * 
   * @param propertyNames A string array of all the property names passed in to the obFlushDirty method. @param
   * previousState The Object array representing the previous state of the properties named in the propertyNames
   * array. @param currentState The Object array representing the current state of the properties named in the
   * propertyNames array. @return The DeltaSet representing the changes encountered in the property states.
   */
  public static DeltaSet calculateDeltaSet(SessionFactory sf, Set<String> validPropertyNames,
      Set<String> invalidPropertyNames,
      Serializable entityId, Object entity, String[] propertyNames, Object[] previousState, Object[] currentState)
  {
    if (previousState == null) {
      previousState = new Object[currentState.length];
    }
    if (propertyNames == null || currentState == null) {
      throw new IllegalArgumentException("All three arrays passed to calculate a delta-set must be non-null");
    }
    if (propertyNames.length != previousState.length && previousState.length != currentState.length) {
      throw new IllegalArgumentException("All three arrays passed to calculate a delta-set must be of the same length");
    }

    DeltaSet deltaSet = new DeltaSet();
    deltaSet.setEntity(entity.getClass());
    deltaSet.setId(entityId);
    try {
      checkProperties(entity, sf, validPropertyNames, invalidPropertyNames, propertyNames, previousState, currentState,
          deltaSet);
    } catch (Throwable t) {
      log.error("Error determining delta-set", t);
    } finally {
      log.debug("Done delta-set determination");
    }
    return deltaSet;
  }

  /**
   * @param propertyNames
   * @param previousState
   * @param currentState
   * @param deltaSet
   */
  private static void checkProperties(Object entity, SessionFactory sf, Set<String> validPropertyNames,
      Set<String> invalidPropertyNames,
      String[] propertyNames, Object[] previousState, Object[] currentState, DeltaSet deltaSet)
  {
    Class<?> propertyType = null;
    for (int i = 0; i < propertyNames.length; i++) {
      String property = propertyNames[i];
      if (validPropertyNames != null && validPropertyNames.contains(property) == false) {
        log.debug("ignoring not valid property [" + property + "]");
        continue;
      }
      if (invalidPropertyNames != null && invalidPropertyNames.contains(property) == true) {
        log.debug("ignoring invalid property [" + property + "]");
        continue;
      }
      if (log.isDebugEnabled() == true) {
        log.debug("Starting property [" + property + "]");
      }
      propertyType = null;
      final Object propertyPreviousState = previousState[i];
      final Object propertyCurrentState = currentState[i];
      final boolean wasPreviousNull = propertyPreviousState == null;
      final boolean isCurrentNull = propertyCurrentState == null;

      if (wasPreviousNull && isCurrentNull) {
        log.debug("Both were null; skipping");
        continue;
      }

      // Try to determine the property type from either currentState or,
      // previousState... Side-note: if both are null, we cannot determine
      // the propertyType, but thats OK as no change has occurred (null==null)
      if (!isCurrentNull) {
        propertyType = propertyCurrentState.getClass();
      } else if (!wasPreviousNull) {
        propertyType = propertyPreviousState.getClass();
      }

      if (propertyType == null) {
        log.debug("Unable to determine property type; continuing");
        continue;
      }

      if (Hibernate.isInitialized(propertyPreviousState) || Hibernate.isInitialized(propertyCurrentState)) {
        final PropertyDelta delta = getDeltaOrNull(entity, sf, propertyNames[i], propertyType, propertyPreviousState,
            propertyCurrentState);
        if (delta != null) {
          deltaSet.addDelta(delta);
        }
      }
    }
  }

  /**
   * General use DeltaSet caluclator.
   */
  public static DeltaSet calculateDeltaSet(Object entity, SessionFactory sf, Serializable entityId, Class<?> entityType,
      Object obj1,
      Object obj2)
  {
    if (obj1 == null || obj2 == null) {
      throw new IllegalArgumentException("Both objects passed to calculate a delta-set must be non-null");
    }

    DeltaSet deltaSet = new DeltaSet();
    deltaSet.setEntity(entityType);
    deltaSet.setId(entityId);
    try {
      BeanInfo beanInfo = Introspector.getBeanInfo(obj1.getClass(), Object.class);
      PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();

      for (int i = 0; i < pds.length; i++) {
        final String propertyName = pds[i].getName();
        final Class<?> propertyType = pds[i].getPropertyType();
        final Object oldValue = PropertyUtils.getProperty(obj1, propertyName);
        final Object newValue = PropertyUtils.getProperty(obj2, propertyName);

        final PropertyDelta delta = getDeltaOrNull(entity, sf, propertyName, propertyType, oldValue, newValue);
        if (delta != null) {
          deltaSet.addDelta(delta);
        }
      }
    } catch (Throwable t) {
      log.error("Error determining delta-set", t);
    } finally {
      log.debug("Done delta-set determination");
    }
    return deltaSet;
  }

  public static PropertyDelta getDeltaOrNull(Object entity, SessionFactory sf, String propertyName,
      Class<?> propertyType,
      Object oldValue, Object newValue)
  {
    PropertyDelta delta = null;
    log.debug("Checking property [name=" + propertyName + ", type=" + propertyType + "]");
    // TODO HISTORY
    //    while (Enhancer.isEnhanced(propertyType)) {
    //      propertyType = propertyType.getSuperclass();
    //    }
    try {
      if (Collection.class.isAssignableFrom(propertyType)) {
        log.debug("Encountered property is a collection type");
        delta = getCollectionDelta(entity, (SessionFactoryImplementor) sf, propertyName, propertyType, oldValue,
            newValue, delta);
      } else if (propertyType.isArray()) {
        log.debug("Encountered property is an array type");
        delta = getArrayDelta(sf, propertyName, propertyType, oldValue, newValue, delta);
      } else if (sf.getClassMetadata(propertyType) != null) {
        log.debug("Encountered property is an association type");
        if (!areEqual(oldValue, newValue, sf)) {
          delta = new AssociationPropertyDelta(sf, propertyName, propertyType, oldValue, newValue);
        }
      } else {
        log.debug("Property was a simple property");
        if (!areEqual(oldValue, newValue, sf)) {
          delta = new SimplePropertyDelta(propertyName, propertyType, oldValue, newValue);
        }
      }
    } catch (HibernateException ex) {
      log.warn("Can't retrieve metadata for" + propertyType.getName());
    }
    if (delta == null) {
      log.debug("No delta occurred");
    } else {
      log.debug("Delta encountered");
    }
    return delta;
  }

  private static Object convertElement(final SessionFactory factory, Object element)
  {
    if (element == null) {
      return null;
    }
    try {
      ClassMetadata classMetadata = factory.getClassMetadata(element.getClass());
      if (classMetadata != null) {
        return classMetadata.getIdentifier(element/* , EntityMode.POJO */);
      }
    } catch (HibernateException ex) {
      log.error("Exception encountered " + ex, ex);
      return null;
    }
    return element;
  }

  @SuppressWarnings("unchecked")
  private static Collection<Object> convertCollection(final SessionFactory factory, Collection<Object> coll)
  {
    if (coll == null) {
      return Collections.EMPTY_SET;
    }
    List<Object> convList = new ArrayList<Object>(coll.size());
    for (Object o : coll) {
      convList.add(convertElement(factory, o));
    }
    return convList;
  }

  /**
   * @param propertyName
   * @param propertyType
   * @param oldValue
   * @param newValue
   * @param delta
   * @return
   */
  private static PropertyDelta getArrayDelta(SessionFactory factory, String propertyName, Class<?> propertyType,
      Object oldValue,
      Object newValue, PropertyDelta delta)
  {
    Collection<Object> oldList = Arrays.asList((Object[]) oldValue);
    Collection<Object> newList = Arrays.asList((Object[]) newValue);
    oldList = convertCollection(factory, oldList);
    newList = convertCollection(factory, newList);

    CollectionPropertyDelta collectionDelta = new CollectionPropertyDelta(propertyName, propertyType, oldList, newList);
    if (collectionDelta.anyChangeDetected()) {
      delta = collectionDelta;
    }
    collectionDelta = null;
    return delta;
  }

  /**
   * @param propertyName
   * @param propertyType
   * @param oldValue
   * @param newValue
   * @param delta
   * @return
   */
  @SuppressWarnings("unchecked")
  private static PropertyDelta getCollectionDelta(Object entity, SessionFactoryImplementor factory, String propertyName,
      Class propertyType, Object oldValue, Object newValue, PropertyDelta delta)
  {
    Collection<Object> oldCollectionValue = (Collection<Object>) oldValue;
    Collection<Object> newCollectionValue = (Collection<Object>) newValue;
    if (oldCollectionValue instanceof PersistentCollection) {
      PersistentCollection persColl = (PersistentCollection) oldCollectionValue;
      Serializable storedSnapshot = persColl.getStoredSnapshot();
      if (storedSnapshot instanceof Map) {
        oldCollectionValue = ((Map) storedSnapshot).keySet();
      }
      if (storedSnapshot instanceof Collection) {
        oldCollectionValue = (Collection<Object>) storedSnapshot;
      }
    }

    if (Hibernate.isInitialized(oldCollectionValue) && Hibernate.isInitialized(newCollectionValue)) {
      CollectionPropertyDelta collectionDelta = null;
      Class returnedClass = Object.class;
      try {
        CollectionType propertyType2 = (CollectionType) factory.getClassMetadata(entity.getClass())
            .getPropertyType(propertyName);
        returnedClass = propertyType2.getElementType(factory).getReturnedClass();
      } catch (QueryException ex) {
        if (oldCollectionValue != null && oldCollectionValue.isEmpty() == false) {
          returnedClass = oldCollectionValue.iterator().next().getClass();
        }
        if (newCollectionValue != null && newCollectionValue.isEmpty() == false) {
          returnedClass = newCollectionValue.iterator().next().getClass();
        }
      }
      oldCollectionValue = convertCollection(factory, oldCollectionValue);
      newCollectionValue = convertCollection(factory, newCollectionValue);
      collectionDelta = new CollectionPropertyDelta(propertyName, returnedClass, oldCollectionValue,
          newCollectionValue);
      if (collectionDelta.anyChangeDetected()) {
        delta = collectionDelta;
      }
      collectionDelta = null;
    } else {
      log.debug("One (or both) of a collection property was not previously initialized; have to skip");
    }
    return delta;
  }

  public static boolean areEqual(Object obj1, Object obj2, SessionFactory sf)
  {
    if (obj1 == null && obj2 == null) {
      log.debug("Both were null");
      return true;
    } else if (obj1 == null || obj2 == null) {
      log.debug("One or the other were null (but not both)");
      return false;
    } else if ((Date.class.isAssignableFrom(obj1.getClass()))
        || (Timestamp.class.isAssignableFrom(obj1.getClass()))
        || (java.sql.Date.class.isAssignableFrom(obj1.getClass()))
        || (Time.class.isAssignableFrom(obj1.getClass()))) {
      Date d1 = (Date) obj1;
      Date d2 = (Date) obj2;
      return d1.equals(d2) || d2.equals(d1);
    } else if (BigDecimal.class.isAssignableFrom(obj1.getClass()) == true) {
      // Use compareTo instead of equals (for ignoring the scale):
      return ((BigDecimal) obj1).compareTo((BigDecimal) obj2) == 0;
    } else {
      log.debug("Checking [" + obj1 + "] against [" + obj2 + "]");
      return areEntitiesEqual(obj1, obj2, sf);
    }
  }

  /**
   * @param obj1
   * @param obj2
   * @param sf
   * @return
   */
  private static boolean areEntitiesEqual(Object obj1, Object obj2, SessionFactory sf)
  {
    try {
      // compare the database identifier
      ClassMetadata clazz = sf.getClassMetadata(obj1.getClass());
      if (clazz != null) {
        if (clazz.hasIdentifierProperty() == true) {
          if (clazz.getIdentifier(obj1/* , EntityMode.POJO */)
              .equals(clazz.getIdentifier(obj2/* , EntityMode.POJO */)) == true) {
            return true;
          }
        }
      }
    } catch (Exception ex) {
      log.error("Exception occured:" + ex, ex);
    }

    return obj1.equals(obj2);
  }
}
