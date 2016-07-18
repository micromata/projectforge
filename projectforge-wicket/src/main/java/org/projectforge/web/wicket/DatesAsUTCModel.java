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

package org.projectforge.web.wicket;

import java.util.Date;

import org.apache.wicket.model.Model;
import org.projectforge.framework.time.DateHelper;

/**
 * Displays from and to date as UTC time stamp. Use-ful for checking the correctness of the time zone of any date object
 * in the UI.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class DatesAsUTCModel extends Model<String>
{
  private static final long serialVersionUID = 3910588105442026807L;

  /**
   * @see org.apache.wicket.model.Model#getObject()
   */
  @Override
  public String getObject()
  {
    final StringBuffer buf = new StringBuffer();
    buf.append("start=[");
    if (getStartTime() != null) {
      buf.append(DateHelper.TECHNICAL_ISO_UTC.get().format(getStartTime()));
    } else {
      buf.append("null");
    }
    buf.append("]; stop=[");
    if (getStopTime() != null) {
      buf.append(DateHelper.TECHNICAL_ISO_UTC.get().format(getStopTime()));
    } else {
      buf.append("null");
    }
    buf.append("]");
    return buf.toString();
  }

  public Date getStartTime()
  {
    return null;
  }

  public Date getStopTime()
  {
    return null;
  }

}
