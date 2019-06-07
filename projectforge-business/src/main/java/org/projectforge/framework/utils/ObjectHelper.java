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

import java.math.BigDecimal;

public class ObjectHelper
{
  public static boolean isEmpty(final Object... values)
  {
    if (values == null) {
      return true;
    }
    for (final Object value : values) {
      if (value == null) {
        continue;
      } else if (value instanceof String) {
        if (((String) value).trim().length() > 0) {
          return false;
        }
      } else if (value instanceof BigDecimal) {
        if (((BigDecimal) value).compareTo(BigDecimal.ZERO) != 0) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }
}
