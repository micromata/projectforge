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

package org.projectforge.business.meb;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.i18n.I18nEnum;

/**
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 *         <ul>
 *         <li>DONE - Issue done / finished.</li>
 *         <li>OPEN - Work in progress.</li>
 *         <li>IMPORTANT - Important issue.</li>
 *         </ul>
 */
public enum MebEntryStatus implements I18nEnum
{
  UNKNOWN("unknown"), RECENT("recent"), DONE("done"), OPEN("open"), IMPORTANT("important");

  private String key;

  public static MebEntryStatus get(String s)
  {
    if (StringUtils.isEmpty(s) == true) {
      return null;
    }
    if ("RECENT".equals(s) == true) {
      return RECENT;
    } else if ("DONE".equals(s) == true) {
      return DONE;
    } else if ("OPEN".equals(s) == true) {
      return OPEN;
    } else if ("IMPORTANT".equals(s) == true) {
      return IMPORTANT;
    } else {
      return UNKNOWN;
    }
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  public String getKey()
  {
    return key;
  }

  @Override
  public String getI18nKey()
  {
    return "meb.entry.status." + key;
  }

  MebEntryStatus(String key)
  {
    this.key = key;
  }
}
