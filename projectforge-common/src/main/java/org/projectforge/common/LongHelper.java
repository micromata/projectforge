/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
public class LongHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LongHelper.class);

  /**
   * Parses the given string as long value.
   *
   * @param value The string representation of the long value to parse.
   * @return Long value or null if an empty string was given or a syntax error occurs.
   */
  public static Long parseLong(String value)
  {
    if (value == null) {
      return null;
    }
    value = value.trim();
    if (value.isEmpty()) {
      return null;
    }
    Long result = null;
    try {
      result = Long.valueOf(value);
    } catch (final NumberFormatException ex) {
      log.warn("Can't parse long: '" + value + "'.");
    }
    return result;
  }
}
