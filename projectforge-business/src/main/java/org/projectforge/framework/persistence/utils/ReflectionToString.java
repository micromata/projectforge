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

package org.projectforge.framework.persistence.utils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.TimeZone;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.Hibernate;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable;

/**
 * 
 * @author wolle (wolle@micromata.de)
 * 
 */
public class ReflectionToString extends ReflectionToStringBuilder
{
  public ReflectionToString(final Object arg0)
  {
    super(Hibernate.isInitialized(arg0) ? arg0 : "Lazy" + arg0.getClass() + "@" + System.identityHashCode(arg0));
  }

  @Override
  public ToStringBuilder append(final String fieldName, final Object object)
  {
    if (object != null) {
      if (Hibernate.isInitialized(object) == false) {
        if (BaseDO.class.isAssignableFrom(object.getClass()) == true) {
          // Work around for Jassist bug:
          final Serializable id = HibernateUtils.getIdentifier((BaseDO< ? >) object);
          return super.append(fieldName, id != null ? id : "<id>");
        }
        return super.append(fieldName, "LazyCollection");
      } else if (ShortDisplayNameCapable.class.isAssignableFrom(object.getClass()) == true) {
        return super.append(fieldName, myToString(object));
      } else if (BaseDO.class.isAssignableFrom(object.getClass()) == true) {
        return super.append(fieldName, myToString(object));
      } else if (object instanceof Collection) {
        final StringBuilder sb = new StringBuilder().append("[");
        boolean first = true;
        for (final Object el : (Collection< ? >) object) {
          if (first == true)
            first = false;
          else sb.append(", ");
          sb.append(myToString(el));
        }
        return super.append(fieldName, sb.append("]").toString());
      } else if (object instanceof TimeZone) {
        return super.append(fieldName, ((TimeZone) object).getID());
      }
    }
    return super.append(fieldName, object);
  }

  private String myToString(final Object obj)
  {
    if (obj == null) {
      return "<null>";
    } else if (ShortDisplayNameCapable.class.isAssignableFrom(obj.getClass()) == true) {
      if (BaseDO.class.isAssignableFrom(obj.getClass()) == true) {
        final Serializable id = HibernateUtils.getIdentifier((BaseDO< ? >) obj);
        return id + ":" + ((ShortDisplayNameCapable) obj).getShortDisplayName();
      }
      return ((ShortDisplayNameCapable) obj).getShortDisplayName();
    } else if (BaseDO.class.isAssignableFrom(obj.getClass()) == true) {
      final Serializable id = HibernateUtils.getIdentifier((BaseDO< ? >) obj);
      return id != null ? id.toString() : "<id>";
    }
    return obj.toString();
  }

  @Override
  protected boolean accept(final Field field)
  {
    try {
      final Object value = getValue(field);
      if (Hibernate.isInitialized(value) == false) {
        append(field.getName(), value);
        return false;
      }
    } catch (final IllegalArgumentException ex) {
      return false;
    } catch (final IllegalAccessException ex) {
      return false;
    }
    return super.accept(field);
  }

  public static String asString(final Object o)
  {
    return new ReflectionToString(o).toString();
  }
}
