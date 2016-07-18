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

package org.projectforge.framework.persistence.database;

import java.util.ArrayList;
import java.util.List;

/**
 * Static helper methods for DatabaseUpdateDao
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatabaseUpdateHelper
{
  /**
   * Tries to get the id from a serialized data base object (BaseDO) serialized with toString().
   * @param serializedObject
   * @return
   */
  public static Integer getId(final String serializedObject)
  {
    if (serializedObject == null) {
      return null;
    }
    final int length = serializedObject.length();
    final Integer[] positions = indexOf(serializedObject, "id=");
    if (positions == null || positions.length == 0) {
      return null;
    }
    int pos = positions[0];
    if (positions.length > 1) {
      // No, we have to find the correct id=xxx!
      for (int p : positions) {
        if (getDepth(serializedObject, p) == 1) {
          pos = p;
          break;
        }
      }
    }
    int toPos = pos + 3;
    while (toPos < length && Character.isDigit(serializedObject.charAt(toPos))) {
      toPos++;
    }
    if (toPos == pos + 3) {
      // "id=" isn't followed by an integer.
      return null;
    }
    final Integer result = new Integer(serializedObject.substring(pos + 3, toPos));
    return result;
  }

  /**
   * Helper method for getId(String). The depth is defined by the brackets '[' and ']'.
   * @param str
   * @param pos
   * @return
   */
  private static int getDepth(final String str, final int pos)
  {
    int depth = 0;
    for (int i = 0; i < pos && i < str.length(); i++) {
      final char ch = str.charAt(i);
      if (ch == '[') {
        ++depth;
      } else if (ch == ']') {
        --depth;
      }
    }
    return depth;
  }

  /**
   * Helper method for getId(String).
   * @param str
   * @param searchString
   * @return
   */
  private static Integer[] indexOf(final String str, final String searchString)
  {
    if (str == null || str.indexOf(searchString) < 0) {
      return null;
    }
    final List<Integer> list = new ArrayList<Integer>();
    int pos = -1;
    final int length = str.length();
    for (int i = 0; i < 100; i++) {
      // Endless loop detection
      if (pos >= length - 1) {
        break;
      }
      pos = str.indexOf(searchString, pos + 1);
      boolean syntax = true;
      if (pos >= 0) {
        // Check that a comma or [ bracket is left to the id=
        for (int j = pos - 1; j >= 0; j--) {
          final char ch = str.charAt(j);
          if (ch == ',' || ch == '[') {
            break;
          }
          if (Character.isWhitespace(ch) == false) {
            syntax = false;
            break;
          }
        }
        if (syntax == true) {
          list.add(pos);
        }
      } else {
        break;
      }
    }
    final Integer[] result = new Integer[list.size()];
    return list.toArray(result);
  }
}
