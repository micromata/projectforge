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

package org.projectforge.business.fibu.kost.reporting;

import org.apache.commons.lang.StringUtils;

public enum ReportOutputType
{
  PDF("pdf");

  private String key;

  /**
   * @see #ordinal()
   */
  public int getOrdinal()
  {
    return ordinal();
  }

  /**
   * @param s Key or toString value of the type, e. g. "pdf" or "PDF".
   * @return Matching ReportOutputType.
   * @throws UnsupportedOperationException if no type match.
   */
  public static ReportOutputType getType(String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    for (ReportOutputType type : ReportOutputType.values()) {
      if (s.equals(type.toString()) == true || s.equals(type.getKey()) == true) {
        return type;
      }
    }
    throw new UnsupportedOperationException("Unknown reporting type: '" + s + "'");
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  ReportOutputType(String key)
  {
    this.key = key;
  }

}
