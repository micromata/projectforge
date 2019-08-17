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

package org.projectforge.common.props;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.anots.PropertyInfo;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class PropUtils {
  private static Field[] EMPTY_FIELDS = new Field[0];

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PropUtils.class);

  private static final Map<Class<?>, Field[]> fieldsMap = new HashMap<Class<?>, Field[]>();

  public static PropertyInfo get(final Class<?> clazz, final String property) {
    final Field field = getField(clazz, property);
    if (field != null) {
      return field.getAnnotation(PropertyInfo.class);
    }
    return null;
  }

  public static PropertyInfo get(final Field field) {
    if (field == null) {
      return null;
    }
    return field.getAnnotation(PropertyInfo.class);
  }

  /**
   *
   * @param clazz
   * @param property Nested properties are supported: task.project.title
   * @return
   */
  public static Field getField(final Class<?> clazz, final String property) {
    return getField(clazz, property, false);
  }

  /**
   *
   * @param clazz
   * @param property Nested properties are supported: task.project.title
   * @param suppressWarning If true, no warning message will be logged, if property not found.
   * @return
   */
  public static Field getField(final Class<?> clazz, final String property, boolean suppressWarning) {
    String[] nestedProps = StringUtils.split(property, '.');
    Class<?> cls = clazz;
    Field field = null;
    for (String nestedProp : nestedProps) {
      field = null; // Reset field from previous loops.
      final Field[] declaredFields = BeanHelper.getAllDeclaredFields(cls);
      for (final Field declaredField : declaredFields) {
        if (nestedProp.equals(declaredField.getName()) == true) {
          field = declaredField;
          cls = field.getType();
          break;
        }
      }
    }
    if (field == null && !suppressWarning) {
      log.warn("Field '" + clazz.getName() + "." + property + "' not found.");
    }
    return field;
  }

  public static String getI18nKey(final Class<?> clazz, final String property) {
    final PropertyInfo info = get(clazz, property);
    if (info == null) {
      log.error("PropertyInfo not found for field '" + clazz.getName() + "." + property + "' not found.");
      return null;
    }
    return info.i18nKey();
  }

  public static Field[] getPropertyInfoFields(final Class<?> clazz) {
    Field[] fields = fieldsMap.get(clazz);
    if (fields != null) {
      return fields;
    }
    final Field[] declaredFields = BeanHelper.getAllDeclaredFields(clazz);
    final List<Field> result = new LinkedList<Field>();
    for (final Field field : declaredFields) {
      final PropertyInfo propertyInfo = field.getAnnotation(PropertyInfo.class);
      if (propertyInfo != null) {
        result.add(field);
      }
    }
    fields = result.toArray(EMPTY_FIELDS);
    fieldsMap.put(clazz, fields);
    return fields;
  }
}
