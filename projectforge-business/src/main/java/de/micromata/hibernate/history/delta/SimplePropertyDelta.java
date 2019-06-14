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

import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.apache.commons.beanutils.ConvertUtils;
import org.hibernate.Session;

/**
 * @author Wolfgang Jung (w.jung@micromata.de)
 *
 */
//@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value = "P")
public class SimplePropertyDelta extends PropertyDelta
{
  private static final ThreadLocal<SimpleDateFormat> SDF_TIMEDATE = new ThreadLocal<SimpleDateFormat>()
  {
    @Override
    protected SimpleDateFormat initialValue()
    {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    }
  };

  private static final ThreadLocal<SimpleDateFormat> SDF_DATE = new ThreadLocal<SimpleDateFormat>()
  {
    @Override
    protected SimpleDateFormat initialValue()
    {
      return new SimpleDateFormat("yyyy-MM-dd");
    }
  };

  protected SimplePropertyDelta()
  {
    // do nothing
  }

  public SimplePropertyDelta(String propertyName, Class<?> propertyType, Object oldValue, Object newValue)
  {
    this.propertyName = propertyName;
    this.propertyType = propertyType.getName();
    if (oldValue instanceof java.sql.Date) {
      this.oldValue = SDF_DATE.get().format((java.sql.Date) oldValue);
    } else if (newValue instanceof Time) {
      this.newValue = SDF_TIMEDATE.get().format((Date) newValue);
    } else if (oldValue instanceof Date) {
      this.oldValue = SDF_TIMEDATE.get().format((Date) oldValue);
    } else {
      this.oldValue = ConvertUtils.convert(oldValue);
    }

    if (newValue instanceof java.sql.Date) {
      this.newValue = SDF_DATE.get().format((java.sql.Date) newValue);
    } else if (newValue instanceof Time) {
      this.newValue = SDF_TIMEDATE.get().format((Date) newValue);
    } else if (newValue instanceof Date) {
      this.newValue = SDF_TIMEDATE.get().format((Date) newValue);
    } else {
      this.newValue = ConvertUtils.convert(newValue);
    }
  }

  @Override
  public String toString()
  {
    return "change " + propertyName + " old=" + oldValue + " new=" + newValue;
  }

  @Override
  public Object getNewObjectValue(Session session)
  {
    return convertToNative(newValue);
  }

  /**
   * @return
   */
  private Object convertToNative(String value)
  {
    if (value == null || value.equals("")) {
      return null;
    }
    try {
      if ("java.sql.Date".equals(propertyType) == true) {
        return new java.sql.Date(SDF_DATE.get().parse(value).getTime());
      } else if ("java.sql.Time".equals(propertyType) == true) {
        return new Time(SDF_TIMEDATE.get().parse(value).getTime());
      } else if ("java.sql.Timestamp".equals(propertyType) == true) {
        return new Timestamp(SDF_TIMEDATE.get().parse(value).getTime());
      } else if ("java.util.Date".equals(propertyType) == true) {
        return SDF_TIMEDATE.get().parse(value);
      }
      return ConvertUtils.convert(value, Class.forName(propertyType));
    } catch (ClassNotFoundException ex) {
      return value;
    } catch (ParseException e) {
      return value;
    }
  }

  @Override
  public Object getOldObjectValue(Session session)
  {
    return convertToNative(oldValue);
  }
}
