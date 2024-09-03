/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentSet;
import org.hibernate.proxy.HibernateProxy;
import org.projectforge.framework.ToStringUtil;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.EntityCopyStatus;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.api.PFPersistancyBehavior;
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO;
import org.projectforge.framework.time.PFDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Utilities to create compat with BaseDao
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class BaseDaoJpaAdapter {
    private static final Logger log = LoggerFactory.getLogger(BaseDaoJpaAdapter.class);

    @SuppressWarnings("unchecked")
    public static EntityCopyStatus copyValues(BaseDO src, final BaseDO dest, final String... ignoreFields) {
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
        Hibernate.initialize(src);
        if (src instanceof HibernateProxy) {
            src = (BaseDO) ((HibernateProxy) src).getHibernateLazyInitializer()
                    .getImplementation();
        }
        return copyDeclaredFields(src.getClass(), src, dest, ignoreFields);
    }

    public static EntityCopyStatus copyDeclaredFields(final Class<?> srcClazz, final BaseDO<?> src,
                                                      final BaseDO<?> dest,
                                                      final String... ignoreFields) {
        final Field[] fields = srcClazz.getDeclaredFields();
        AccessibleObject.setAccessible(fields, true);
        EntityCopyStatus modificationStatus = EntityCopyStatus.NONE;
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
                        if (destFieldValue instanceof Collection && ((Collection) destFieldValue).isEmpty()) {
                            // dest is an empty collection, so no MAJOR update.
                        } else {
                            field.set(dest, null);
                            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
                        }
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
                        } else if (srcColl instanceof PersistentSet) {
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
                                final EntityCopyStatus st = destEntry.copyValuesFrom((BaseDO<?>) srcEntry);
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
                        log.error("Can't get id though can't copy the BaseDO (see error message above about HHH-3502), or id not given for "
                                + srcFieldValue.getClass() + ": " + ToStringUtil.toJsonString(srcFieldValue));
                    }
                } else if (srcFieldValue instanceof LocalDate) {
                    if (destFieldValue == null) {
                        field.set(dest, srcFieldValue);
                        modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
                    } else {
                        final PFDay srcDay = PFDay.from((LocalDate) srcFieldValue);
                        final PFDay destDay = PFDay.from((LocalDate) destFieldValue);
                        if (!srcDay.isSameDay(destDay)) {
                            field.set(dest, srcDay.getLocalDate());
                            modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
                        }
                    }
                } else if (srcFieldValue instanceof java.sql.Date) {
                    if (destFieldValue == null) {
                        field.set(dest, srcFieldValue);
                        modificationStatus = getModificationStatus(modificationStatus, src, fieldName);
                    } else {
                        final PFDay srcDay = PFDay.from((java.sql.Date) srcFieldValue);
                        final PFDay destDay = PFDay.from((Date) destFieldValue);
                        if (!srcDay.isSameDay(destDay)) {
                            field.set(dest, srcDay.getSqlDate());
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
            final EntityCopyStatus st = copyDeclaredFields(superClazz, src, dest, ignoreFields);
            modificationStatus = getModificationStatus(modificationStatus, st);
        }
        return modificationStatus;
    }

    protected static EntityCopyStatus getModificationStatus(final EntityCopyStatus currentStatus, final BaseDO<?> src,
                                                            final String modifiedField) {
        //PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
        /*
        TODO: Implement this.
        if (historyService.getNoHistoryProperties(emf, src.getClass()).contains(modifiedField)) {
            // This field is not historized, so no major update:
            return EntityCopyStatus.MINOR;
        }*/
        if (currentStatus == EntityCopyStatus.MAJOR
                || !(src instanceof AbstractHistorizableBaseDO)
                || !(src instanceof HibernateProxy)) {
            return EntityCopyStatus.MAJOR;
        }
        log.error("********* to be implemented.");
        return EntityCopyStatus.MINOR;
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
        return true;
    }

    public static EntityCopyStatus getModificationStatus(final EntityCopyStatus currentStatus,
                                                         final EntityCopyStatus status) {
        return currentStatus.combine(status);
    }
}
