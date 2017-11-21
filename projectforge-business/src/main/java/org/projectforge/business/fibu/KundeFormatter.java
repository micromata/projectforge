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

package org.projectforge.business.fibu;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.utils.BaseFormatter;
import org.projectforge.framework.access.AccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KundeFormatter extends BaseFormatter
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(KundeFormatter.class);

  @Autowired
  private KundeDao kundeDao;

  /**
   * Displays customers and/or kundeText as String.
   * 
   * @param kunde null supported.
   * @param kundeText null supported.
   * @return
   */
  public static String formatKundeAsString(final KundeDO kunde, final String kundeText)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    if (StringUtils.isNotBlank(kundeText) == true) {
      first = false;
      buf.append(kundeText);
    }
    if (kunde != null && StringUtils.isNotBlank(kunde.getName()) == true) {
      if (first == true)
        first = false;
      else
        buf.append("; ");
      buf.append(kunde.getName());
    }
    return buf.toString();

  }

  /**
   * @see #format(KundeDO, boolean, boolean)
   */
  public String format(final Integer kundeId, final boolean showOnlyNumber)
  {
    KundeDO kunde = null;
    try {
      kunde = kundeDao.getById(kundeId);
    } catch (AccessException ex) {
      log.info(ex.getMessage());
      return getNotVisibleString();
    }
    return format(kunde, showOnlyNumber);
  }

  /**
   * Formats given project as string.
   * 
   * @param kunde The kunde to show.
   * @param showOnlyNumber If true then only the kost2 number will be shown.
   * @return
   */
  public String format(final KundeDO kunde, final boolean showOnlyNumber)
  {
    if (kunde == null) {
      return "";
    }
    StringBuffer sb = new StringBuffer();

    boolean hasAccess = kundeDao.hasLoggedInUserSelectAccess(false);
    if (hasAccess == false) {
      appendNotVisible(sb);
    } else {
      if (showOnlyNumber == true) {
        sb.append(KostFormatter.format(kunde));
      } else {
        sb.append(KostFormatter.formatKunde(kunde));
      }
    }
    return sb.toString();
  }

}
