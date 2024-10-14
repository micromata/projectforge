/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.utils;

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import java.io.Serializable;

public class BaseFormatter implements Serializable
{
  /**
   * Appends <not visible> element (italic and gray colored) to the given StringBuilder. This is used by the text for displaying not
   * accessible fields.
   */
  public void appendNotVisible(StringBuilder sb)
  {
    sb.append("<span style=\"font-style:italic; color: gray;\">&lt;").append(getLocalizedString("notVisible")).append("&gt;</span>");
  }

  public String getNotVisibleString()
  {
    StringBuilder sb = new StringBuilder();
    appendNotVisible(sb);
    return sb.toString();
  }

  /**
   * Proxy.
   * @param key
   * @return
   * @see ThreadLocalUserContext#getLocalizedString(String)
   */
  public String getLocalizedString(String key)
  {
    return ThreadLocalUserContext.getLocalizedString(key);
  }

}
