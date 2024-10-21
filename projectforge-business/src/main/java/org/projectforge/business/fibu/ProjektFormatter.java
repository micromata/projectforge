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

package org.projectforge.business.fibu;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.utils.BaseFormatter;
import org.projectforge.framework.access.AccessException;
import org.projectforge.web.WicketSupport;
import org.springframework.stereotype.Service;

@Service
public class ProjektFormatter extends BaseFormatter
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjektFormatter.class);

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
    final StringBuilder buf = new StringBuilder();
    boolean first = true;
    if (StringUtils.isNotBlank(kundeText)) {
      first = false;
      buf.append(kundeText);
    }
    if (kunde != null && StringUtils.isNotBlank(kunde.getName())) {
      if (first)
        first = false;
      else
        buf.append("; ");
      buf.append(kunde.getName());
    }
    if (projekt != null) {
      // Show kunde name only, if not already given in buffer before.
      if (projekt.getKunde() != null && !StringUtils.contains(buf.toString(), projekt.getKunde().getName())) {
        if (first)
          first = false;
        else
          buf.append("; ");
        buf.append(projekt.getKunde().getName());
      }
      if (StringUtils.isNotBlank(projekt.getName())) {
        if (first)
          first = false;
        else
          buf.append(" - ");
        buf.append(projekt.getName());
      }
    }
    return buf.toString();
  }

  public String format(final Integer projektId, final boolean showOnlyNumber)
  {
    ProjektDO projekt = null;
    try {
      projekt = WicketSupport.get(ProjektDao.class).find(projektId);
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
   * @return
   */
  public String format(final ProjektDO projekt, final boolean showOnlyNumber)
  {
    if (projekt == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    // final KundeDO kunde = projekt.getKunde();
    boolean hasAccess = WicketSupport.get(ProjektDao.class).hasLoggedInUserSelectAccess(false);
    if (!hasAccess) {
      return null;
    } else if (projekt != null) {
      if (showOnlyNumber) {
        sb.append(KostFormatter.getInstance().formatProjekt(projekt, KostFormatter.FormatType.FORMATTED_NUMBER));
      } else {
        sb.append(KostFormatter.getInstance().formatProjekt(projekt, KostFormatter.FormatType.TEXT));
      }
    }

    return sb.toString();
  }

}
