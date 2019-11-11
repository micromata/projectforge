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

package org.projectforge.framework.persistence.jpa.impl;

import de.micromata.genome.db.jpa.history.api.HistoryService;
import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;
import de.micromata.genome.db.jpa.tabattr.api.EntityWithTimeableAttr;
import de.micromata.genome.db.jpa.tabattr.api.TimeableAttrRow;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.jpa.events.EmgrInitForInsertEvent;
import de.micromata.genome.jpa.events.EmgrInitForUpdateEvent;
import de.micromata.genome.jpa.events.impl.InitCreatedStdRecordFieldsEventHandler;
import de.micromata.genome.jpa.events.impl.InitUpdateStdRecordFieldsEventHandler;
import de.micromata.genome.util.bean.FieldMatchers;
import de.micromata.genome.util.bean.PrivateBeanUtils;
import de.micromata.genome.util.matcher.CommonMatchers;
import de.micromata.genome.util.matcher.Matcher;
import de.micromata.genome.util.matcher.MatcherBase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.proxy.HibernateProxy;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.api.*;
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO;
import org.projectforge.framework.persistence.hibernate.HibernateCompatUtils;
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.time.DayHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utilities to create compat with BaseDao
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class BaseDaoJpaAdapter {
  private static final Logger log = LoggerFactory.getLogger(BaseDaoJpaAdapter.class);

  public static void prepareInsert(ExtendedBaseDO<?> dbObj) {
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    emf.runInTrans((emgr) -> {
      EmgrInitForInsertEvent nev = new EmgrInitForInsertEvent(emgr, dbObj);
      new InitCreatedStdRecordFieldsEventHandler().onEvent(nev);
      return null;
    });

  }

  public static void inserted(ExtendedBaseDO<?> dbObj) {
    HistoryBaseDaoAdapter.inserted(dbObj);
  }

  public static void beforeUpdateCopyMarkDelete(ExtendedBaseDO<?> dbObj, ExtendedBaseDO<?> obj) {
    HistoryBaseDaoAdapter.markedAsDeleted(dbObj, obj);
  }

  public static void beforeUpdateCopyMarkUnDelete(ExtendedBaseDO<?> dbObj, ExtendedBaseDO<?> obj) {
    HistoryBaseDaoAdapter.markedAsUnDeleted(dbObj, obj);
  }

  public static void prepareUpdate(ExtendedBaseDO<?> dbObj) {
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    emf.runInTrans((emgr) -> {
      EmgrInitForUpdateEvent nev = new EmgrInitForUpdateEvent(emgr, dbObj);
      new InitUpdateStdRecordFieldsEventHandler().onEvent(nev);

      return null;
    });
  }

  public static void updated(ExtendedBaseDO<?> dbObj, ExtendedBaseDO<?> obj) {

  }

  @SuppressWarnings("unchecked")
  public static ModificationStatus copyValues(final BaseDO src, final BaseDO dest, final String... ignoreFields) {
    if (!ClassUtils.isAssignable(src.getClass(), dest.getClass())) {
      throw new RuntimeException("Try to copyValues from different BaseDO classes: this from type "
              + dest.getClass().getName()
              + " and src from type"
              + src.getClass().getName()
              + "!");
    }
    if (src.getId() != null && (ignoreFields == null || !ArrayUtils.contains(ignoreFields, "id"))) {
      dest.setId(src.getId());
    }
    return copyDeclaredFields(src.getClass(), src, dest, ignoreFields);
  }

  @SuppressWarnings("rawtypes")
  public static ModificationStatus copyValues(JpaTabAttrBaseDO destEntry, JpaTabAttrBaseDO sourceEntry,
                                              String... ignoreFields) {
    ModificationStatus modificationStatus = ModificationStatus.NONE;
    String tsd = destEntry.getStringData();
    JpaTabAttrBaseDO<?, ?> src = sourceEntry;
    String ssd = src.getStringData();
    if (StringUtils.equals(tsd, ssd)) {
      return modificationStatus;
    }
    destEntry.getData().clear();
    destEntry.getData().addAll(src.getData());
    modificationStatus = modificationStatus.combine(ModificationStatus.MAJOR);
    return modificationStatus;
  }

  /**
   * Merges timed attributes.
   *
   * @param target
   * @param source
   * @return
   */
  public static <PK extends Serializable, T extends TimeableAttrRow<PK>> ModificationStatus copyTimeableAttribute(
          final EntityWithTimeableAttr<PK, T> target,
          final EntityWithTimeableAttr<PK, T> source) {
    ModificationStatus mod = ModificationStatus.NONE;

    Set<Serializable> sourcePks = source
            .getTimeableAttributes()
            .stream()
            .map(T::getPk)
            .collect(Collectors.toSet());

    Map<Serializable, T> targetRows = target
            .getTimeableAttributes()
            .stream()
            .collect(Collectors.toMap(T::getPk, o -> o));

    // copy rows from source to target
    for (T sourceRow : source.getTimeableAttributes()) {
      if (sourceRow.getPk() == null || !targetRows.containsKey(sourceRow.getPk())) {
        mod = mod.combine(ModificationStatus.MAJOR);
        target.addTimeableAttribute(sourceRow);
        continue;
      }
      T targetRow = targetRows.get(sourceRow.getPk());
      mod = mod.combine(copyDeclaredSimpleFields(targetRow.getClass(), targetRow, sourceRow, "createdAt", "createdBy", "updateCounter"));
      mod = mod.combine(BaseDaoJpaAdapter.copyTabAttributes(targetRow, sourceRow));
    }

    // remove deleted rows from target, these are rows which are not in source but in target
    for (Iterator<T> lis = target.getTimeableAttributes().iterator(); lis.hasNext(); ) {
      T tagetrow = lis.next();
      if (!sourcePks.contains(tagetrow.getPk())) {
        mod = mod.combine(ModificationStatus.MAJOR);
        lis.remove();
      }
    }

    return mod;
  }

  @SuppressWarnings("unchecked")
  public static ModificationStatus copyDeclaredSimpleFields(Class<?> srcClazz, Object dest, Object src,

                                                            String... ignoreFields) {

    ModificationStatus mod = ModificationStatus.NONE;
    Matcher<Field> matcher = CommonMatchers.and(
            FieldMatchers.hasNotModifier(Modifier.STATIC),
            FieldMatchers.hasNotModifier(Modifier.TRANSIENT),
            new MatcherBase<Field>() {
              @Override
              public boolean match(Field object) {
                return !ArrayUtils.contains(ignoreFields, object.getName());
              }
            },
            CommonMatchers.not(
                    CommonMatchers.or(
                            FieldMatchers.assignableTo(Collection.class),
                            FieldMatchers.assignableTo(Map.class),
                            FieldMatchers.assignableTo(DbRecord.class),
                            FieldMatchers.assignableTo(IdObject.class))));
    List<Field> foundFields = PrivateBeanUtils.findAllFields(srcClazz, matcher);

    for (Field field : foundFields) {
      Object svalue = PrivateBeanUtils.readField(src, field);
      Object tvalue = PrivateBeanUtils.readField(dest, field);
      if (!Objects.equals(svalue, tvalue)) {
        mod = mod.combine(ModificationStatus.MAJOR);
      }
      PrivateBeanUtils.writeField(dest, field, svalue);
    }
    return mod;
  }

  public static ModificationStatus copyTabAttributes(EntityWithAttributes target, EntityWithAttributes source) {
    ModificationStatus mod = ModificationStatus.NONE;

    //    for (String key : source.getAttributeKeys()) {
    //
    //      String targetValue = target.getStringAttribute(key);
    //      String sourceValue = source.getStringAttribute(key);
    //      if (StringUtils.equals(sourceValue, targetValue) == true) {
    //        continue;
    //      }
    //      target.putStringAttribute(key, sourceValue);
    //      mod = mod.combine(ModificationStatus.MAJOR);
    //
    //    }
    //    final ArrayList<String> keys = new ArrayList<>();
    //    keys.addAll(target.getAttributeKeys());
    //    for (final String key : keys) {
    //      if (source.getAttributeKeys().contains(key) == false) {
    //        target.removeAttribute(key);
    //        mod.combine(ModificationStatus.MAJOR);
    //      }
    //    }

    Set<String> destKeys = target.getAttributeKeys();
    Set<String> origKeys = source.getAttributeKeys();
    Set<String> insertOrgs = new TreeSet<>(origKeys);
    insertOrgs.removeAll(destKeys);
    Set<String> updateOrgs = new TreeSet<>(origKeys);
    updateOrgs.retainAll(destKeys);
    Set<String> deleteOrgs = new TreeSet<>(destKeys);
    deleteOrgs.removeAll(origKeys);
    for (String insert : insertOrgs) {
      target.putAttribute(insert, source.getAttribute(insert));
      mod = mod.combine(ModificationStatus.MAJOR);
    }
    for (String update : updateOrgs) {
      Object sval = source.getAttribute(update);
      Object tval = target.getAttribute(update);
      if (Objects.equals(sval, tval)) {
        mod = mod.combine(ModificationStatus.MAJOR);
      }
      target.putAttribute(update, source.getAttribute(update));
    }
    for (String delete : deleteOrgs) {
      target.removeAttribute(delete);
      mod = mod.combine(ModificationStatus.MAJOR);
    }

    return mod;
  }

  public static ModificationStatus copyDeclaredFields(final Class<?> srcClazz, final BaseDO<?> src,
                                                      final BaseDO<?> dest,
                                                      final String... ignoreFields) {
    final Field[] fields = srcClazz.getDeclaredFields();
    AccessibleObject.setAccessible(fields, true);
    ModificationStatus modificationStatus = ModificationStatus.NONE;
    for (final Field field : fields) {
      final String fieldName = field.getName();
      if ((ignoreFields != null && ArrayUtils.contains(ignoreFields, fieldName)) || !accept(field)) {
        continue;
      }
      try {
        final Object srcFieldValue = field.get(src);
        final Object destFieldValue = field.get(dest);
        if (field.getType().isPrimitive()) {
          if (!Objects.equals(destFieldValue, srcFieldValue)) {
            field.set(dest, srcFieldValue);
            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
          }
          continue;
        } else if (srcFieldValue == null) {
          if (field.getType() == String.class) {
            if (StringUtils.isNotEmpty((String) destFieldValue)) {
              field.set(dest, null);
              modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
            }
          } else if (destFieldValue != null) {
            field.set(dest, null);
            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
          } else {
            // dest was already null
          }
        } else if (srcFieldValue instanceof Collection) {
          Collection<Object> destColl = (Collection<Object>) destFieldValue;
          final Collection<Object> srcColl = (Collection<Object>) srcFieldValue;
          final Collection<Object> toRemove = new ArrayList<>();
          if (srcColl != null && destColl == null) {
            if (srcColl instanceof TreeSet) {
              destColl = new TreeSet<>();
            } else if (srcColl instanceof HashSet) {
              destColl = new HashSet<>();
            } else if (srcColl instanceof List) {
              destColl = new ArrayList<>();
            } else if (HibernateCompatUtils.isPersistenceSet(srcColl)) {
              destColl = new HashSet<>();
            } else {
              log.error("Unsupported collection type: " + srcColl.getClass().getName());
            }
            field.set(dest, destColl);
          }
          for (final Object o : destColl) {
            if (!srcColl.contains(o)) {
              toRemove.add(o);
            }
          }
          for (final Object o : toRemove) {
            if (log.isDebugEnabled()) {
              log.debug("Removing collection entry: " + o);
            }
            destColl.remove(o);
            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
          }
          for (final Object srcEntry : srcColl) {
            if (!destColl.contains(srcEntry)) {
              if (log.isDebugEnabled()) {
                log.debug("Adding new collection entry: " + srcEntry);
              }
              destColl.add(srcEntry);
              modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
            } else if (srcEntry instanceof BaseDO) {
              final PFPersistancyBehavior behavior = field.getAnnotation(PFPersistancyBehavior.class);
              if (behavior != null && behavior.autoUpdateCollectionEntries()) {
                BaseDO<?> destEntry = null;
                for (final Object entry : destColl) {
                  if (entry.equals(srcEntry)) {
                    destEntry = (BaseDO<?>) entry;
                    break;
                  }
                }
                Validate.notNull(destEntry);
                final ModificationStatus st = destEntry.copyValuesFrom((BaseDO<?>) srcEntry);
                modificationStatus = getModificationStatus(modificationStatus, st);
              }
            }
          }
        } else if (srcFieldValue instanceof BaseDO) {
          final Serializable srcFieldValueId = HibernateUtils.getIdentifier((BaseDO<?>) srcFieldValue);
          if (srcFieldValueId != null) {
            if (destFieldValue == null
                    || !Objects.equals(srcFieldValueId, ((BaseDO<?>) destFieldValue).getId())) {
              field.set(dest, srcFieldValue);
              modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
            }
          } else {
            log.error("Can't get id though can't copy the BaseDO (see error message above about HHH-3502).");
          }
        } else if (srcFieldValue instanceof java.sql.Date) {
          if (destFieldValue == null) {
            field.set(dest, srcFieldValue);
            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
          } else {
            final DayHolder srcDay = new DayHolder((Date) srcFieldValue);
            final DayHolder destDay = new DayHolder((Date) destFieldValue);
            if (!srcDay.isSameDay(destDay)) {
              field.set(dest, srcDay.getSQLDate());
              modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
            }
          }
        } else if (srcFieldValue instanceof Date) {
          if (destFieldValue == null || ((Date) srcFieldValue).getTime() != ((Date) destFieldValue).getTime()) {
            field.set(dest, srcFieldValue);
            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
          }
        } else if (srcFieldValue instanceof BigDecimal) {
          if (destFieldValue == null || ((BigDecimal) srcFieldValue).compareTo((BigDecimal) destFieldValue) != 0) {
            field.set(dest, srcFieldValue);
            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
          }
        } else if (!Objects.equals(destFieldValue, srcFieldValue)) {
          field.set(dest, srcFieldValue);
          modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
        }
      } catch (final IllegalAccessException ex) {
        throw new InternalError("Unexpected IllegalAccessException: " + ex.getMessage());
      }
    }
    final Class<?> superClazz = srcClazz.getSuperclass();
    if (superClazz != null) {
      final ModificationStatus st = copyDeclaredFields(superClazz, src, dest, ignoreFields);
      modificationStatus = getModificationStatus(modificationStatus, st);
    }
    return modificationStatus;
  }

  protected static ModificationStatus getModificationStatus(final ModificationStatus currentStatus, final BaseDO<?> src,
                                                            final String modifiedField) {
    HistoryService historyService = HistoryServiceManager.get().getHistoryService();
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    if (currentStatus == ModificationStatus.MAJOR
            || !(src instanceof AbstractHistorizableBaseDO)
            || (!(src instanceof HibernateProxy)
            && !historyService.getNoHistoryProperties(emf, src.getClass()).contains(modifiedField))) {
      return ModificationStatus.MAJOR;
    }
    return ModificationStatus.MINOR;
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
    if (Modifier.isTransient(field.getModifiers())) {
      // transients.
      return false;
    }
    if (Modifier.isStatic(field.getModifiers())) {
      // transients.
      return false;
    }
    if ("created".equals(field.getName()) || "lastUpdate".equals(field.getName())) {
      return false;
    }
    return true;
  }

  public static ModificationStatus getModificationStatus(final ModificationStatus currentStatus,
                                                         final ModificationStatus status) {
    return currentStatus.combine(status);
  }
}
