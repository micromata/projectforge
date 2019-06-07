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

package org.projectforge.business.fibu.kost;

import org.projectforge.framework.utils.NumberHelper;

public class KostHelper
{
  /**
   * @param kostString Format ######## or #.###.##.## is supported.
   * @return int[4] or null if string is not parse-able.
   */
  public static int[] parseKostString(final String kostString)
  {
    if (kostString == null) {
      return null;
    }
    final String str = kostString.trim();
    if (str.length() == 8) {
      for (int i = 0; i < 8; i++) {
        if (Character.isDigit(str.charAt(i)) == false) {
          return null;
        }
      }
    } else if (str.length() == 11) {
      for (int i = 0; i < 11; i++) {
        if (i == 1 || i == 5 || i == 8) {
          if (str.charAt(i) != '.') {
            return null;
          }
        } else if (Character.isDigit(str.charAt(i)) == false) {
          return null;
        }
      }
    } else {
      return null;
    }
    final int[] result = new int[4];
    result[0] = NumberHelper.parseInteger(kostString.substring(0, 1));
    if (kostString.indexOf('.') > 0) {
      result[1] = NumberHelper.parseInteger(kostString.substring(2, 5));
      result[2] = NumberHelper.parseInteger(kostString.substring(6, 8));
      result[3] = NumberHelper.parseInteger(kostString.substring(9, 11));
    } else {
      result[1] = NumberHelper.parseInteger(kostString.substring(1, 4));
      result[2] = NumberHelper.parseInteger(kostString.substring(4, 6));
      result[3] = NumberHelper.parseInteger(kostString.substring(6, 8));
    }
    return result;
  }
}
