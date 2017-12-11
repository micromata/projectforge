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

package org.projectforge.framework.utils;

public class ClassHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ClassHelper.class);

  /**
   * Returns the default values of primitive types (if given). Boolean: false, Integer: 0.
   * @param type
   * @param value
   * @return Given object if not primitive or not null, otherwise the default value.
   */
  public static Object getDefaultType(final Class< ? > type)
  {
    if (type.isPrimitive() == false) {
      return null;
    }
    if (Boolean.TYPE.equals(type) == true) {
      return false;
    }
    if (Integer.TYPE.equals(type) == true) {
      return 0;
    }
    log.warn("Unsupported type for null value of type: " + type);
    return null;
  }

  /**
   * @param type
   * @param value
   * @return true for null values and for boolean "false" value, otherwise false.
   */
  public static boolean isDefaultType(final Class< ? > type, final Object value)
  {
    if (value == null) {
      return true;
    }
    if ((value instanceof Boolean || Boolean.TYPE.equals(type) == true) && (Boolean) value == false) {
      return true;
    }
    return false;
  }
}
