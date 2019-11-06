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

package org.projectforge.framework.utils;

import org.apache.commons.lang3.ClassUtils;
import org.projectforge.common.BeanHelper;
import org.projectforge.common.anots.StringAlphanumericSort;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import se.sawano.java.text.AlphanumericComparator;

import java.lang.annotation.Annotation;
import java.util.Comparator;

public class MyBeanComparator<T> implements Comparator<T>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MyBeanComparator.class);

  private String property, secondProperty;

  private boolean ascending, secondAscending;

  public MyBeanComparator(final String property)
  {
    this(property, true);

  }

  public MyBeanComparator(final String property, final boolean asc)
  {
    this.property = property;
    this.ascending = asc;
  }

  public MyBeanComparator(final String property, final boolean ascending, final String secondProperty,
      final boolean secondAscending)
  {
    this.property = property;
    this.ascending = ascending;
    this.secondProperty = secondProperty;
    this.secondAscending = secondAscending;
  }

  @Override
  public int compare(final T o1, final T o2)
  {
    final int result = compare(o1, o2, property, ascending);
    if (result != 0) {
      return result;
    }
    return compare(o1, o2, secondProperty, secondAscending);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private int compare(final T o1, final T o2, final String prop, final boolean asc)
  {
    if (prop == null) {
      // Not comparable.
      return 0;
    }
    try {
      final Object value1 = BeanHelper.getNestedProperty(o1, prop);
      final Object value2 = BeanHelper.getNestedProperty(o2, prop);
      if (value1 == null) {
        if (value2 == null)
          return 0;
        else
          return (asc) ? -1 : 1;
      }
      if (value2 == null) {
        return (asc) ? 1 : -1;
      }
      if (value1 instanceof String && value2 instanceof String) {
        if (checkAnnotation(BeanHelper.getDeclaredAnnotations(o1.getClass(), prop), StringAlphanumericSort.class)
            && checkAnnotation(BeanHelper.getDeclaredAnnotations(o2.getClass(), prop), StringAlphanumericSort.class)) {
          AlphanumericComparator alphanumericComparator = new AlphanumericComparator(ThreadLocalUserContext.getLocale());
          if (asc) {
            return alphanumericComparator.compare((String) value2, (String) value1);
          } else {
            return alphanumericComparator.compare((String) value1, (String) value2);
          }
        } else {
          return StringComparator.compare((String) value1, (String) value2, asc);
        }
      }
      if (ClassUtils.isAssignable(value2.getClass(), value1.getClass())) {
        if (asc) {
          return ((Comparable) value1).compareTo(value2);
        } else {
          return -((Comparable) value1).compareTo(value2);
        }
      } else {
        final String sval1 = String.valueOf(value1);
        final String sval2 = String.valueOf(value2);
        if (asc) {
          return sval1.compareTo(sval2);
        } else {
          return -sval1.compareTo(sval2);
        }
      }
    } catch (final Exception ex) {
      log.error("Exception while comparing values of property '" + prop + "': " + ex.getMessage());
      return 0;
    }
  }

  private boolean checkAnnotation(Annotation[] declaredAnnotations, Class<?> annotation)
  {
    if (declaredAnnotations == null) {
      return false;
    }
    for (Annotation a : declaredAnnotations) {
      if (annotation.isInstance(a)) {
        return true;
      }
    }
    return false;
  }
}
