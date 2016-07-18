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

package org.projectforge.common;

/**
 * Some useful methods for handling integer values.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class IntegerHelper
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(IntegerHelper.class);

  /**
   * Parses the given string as integer value.
   * 
   * @param value The string representation of the integer value to parse.
   * @return Integer value or null if an empty string was given or a syntax error occurs.
   */
  public static Integer parseInteger(String value)
  {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.length() == 0) {
      return null;
    }
    Integer result = null;
    try {
      result = new Integer(value);
    } catch (final NumberFormatException ex) {
      log.warn("Can't parse integer: '" + value + "'.");
    }
    return result;
  }
}
