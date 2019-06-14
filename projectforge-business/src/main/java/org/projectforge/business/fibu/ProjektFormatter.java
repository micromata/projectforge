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

package org.projectforge.business.fibu;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.utils.BaseFormatter;
import org.projectforge.framework.access.AccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjektFormatter extends BaseFormatter
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjektFormatter.class);

  @Autowired
  private ProjektDao projektDao;

  /**
   * Formats kunde, kundeText, projekt.kunde and projekt as string.
   * 
   * @param projekt null supported.
   * @param kunde null supported.
   * @param kundeText null supported.
   * @return
   */
  public static String formatProjektKundeAsString(final ProjektDO projekt, final KundeDO kunde, final String kundeText)
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
    if (projekt != null) {
      // Show kunde name only, if not already given in buffer before.
      if (projekt.getKunde() != null && StringUtils.contains(buf.toString(), projekt.getKunde().getName()) == false) {
        if (first == true)
          first = false;
        else
          buf.append("; ");
        buf.append(projekt.getKunde().getName());
      }
      if (StringUtils.isNotBlank(projekt.getName()) == true) {
        if (first == true)
          first = false;
        else
          buf.append(" - ");
        buf.append(projekt.getName());
      }
    }
    return buf.toString();
  }

  /**
   * @see #format(ProjektDO, boolean, String, boolean)
   */
  public String format(final Integer projektId, final boolean showOnlyNumber)
  {
    ProjektDO projekt = null;
    try {
      projekt = projektDao.getById(projektId);
    } catch (AccessException ex) {
      log.info(ex.getMessage());
      return getNotVisibleString();
    }
    return format(projekt, showOnlyNumber);
  }

  /**
   * Formats given project as string.
   * 
   * @param projekt The project to show.
   * @param showOnlyNumber If true then only the kost2 number will be shown.
   * @param select If not empty then the project is selectable for the variable (named via this parameter).
   * @param nullable If true then the unselect button will be shown.
   * @return
   */
  public String format(final ProjektDO projekt, final boolean showOnlyNumber)
  {
    if (projekt == null) {
      return "";
    }
    StringBuffer sb = new StringBuffer();
    // final KundeDO kunde = projekt.getKunde();
    boolean hasAccess = projektDao.hasLoggedInUserSelectAccess(false);
    if (hasAccess == false) {
      return null;
    } else if (projekt != null) {
      if (showOnlyNumber == true) {
        sb.append(KostFormatter.format(projekt));
      } else {
        sb.append(KostFormatter.formatProjekt(projekt));
      }
    }

    return sb.toString();
  }

}
