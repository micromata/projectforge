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

package org.projectforge.business.humanresources;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.framework.utils.NumberHelper;


public class HRViewUserEntryData implements Comparable<HRViewUserEntryData>, Serializable
{
  private static final long serialVersionUID = 7372659070887341614L;

  ProjektDO projekt;

  KundeDO kunde;

  HRPlanningEntryStatus planningEntryStatus;

  long plannedSeconds = 0;

  long actualSeconds = 0;
  
  private String orderString;

  HRViewUserEntryData(final ProjektDO projekt)
  {
    this.projekt = projekt;
  }

  public HRViewUserEntryData(final KundeDO kunde)
  {
    this.kunde = kunde;
  }

  HRViewUserEntryData(final HRPlanningEntryStatus status)
  {
    this.planningEntryStatus = status;
  }

  void addTimesheet(final TimesheetDO timesheet)
  {
    actualSeconds += (timesheet.getDuration() / 1000);
  }

  void addPlanningEntry(final HRPlanningEntryDO entry)
  {
    plannedSeconds += entry.getTotalHours().multiply(NumberHelper.THREE_THOUSAND_SIX_HUNDRED).longValue();
  }

  public BigDecimal getPlannedDays()
  {
    return new BigDecimal(plannedSeconds).divide(new BigDecimal(28800), 2, RoundingMode.HALF_UP);
  }

  public BigDecimal getActualDays()
  {
    return new BigDecimal(actualSeconds).divide(new BigDecimal(28800), 2, RoundingMode.HALF_UP);
  }

  /**
   * Order by id (name or identifier of project or customer). Entries without projects and customer first, then projects and then customers
   * (in alphabetical order).
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   * @see ProjektDO#getProjektIdentifierDisplayName()
   * @see KundeDO#getKundeIdentifierDisplayName()
   */
  public int compareTo(HRViewUserEntryData o)
  {
    return this.getOrderString().compareTo(o.getOrderString());
  }

  private String getOrderString()
  {
    if (orderString == null) {
      if (planningEntryStatus != null) {
        orderString = "1" + this.planningEntryStatus.name();
      } else if (this.projekt != null) {
        orderString = "2" + this.projekt.getProjektIdentifierDisplayName();
      } else if (this.kunde != null) {
        orderString = "3" + this.kunde.getKundeIdentifierDisplayName();
      } else {
        // Should not occur.
        orderString = "";
      }
    }
    return orderString;
  }
}
