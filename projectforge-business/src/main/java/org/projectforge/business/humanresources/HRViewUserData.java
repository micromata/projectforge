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
import java.util.ArrayList;
import java.util.List;

import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.framework.persistence.api.IdObject;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.utils.NumberHelper;

public class HRViewUserData implements Comparable<HRViewUserData>, Serializable, IdObject<Serializable>
{
  private static final long serialVersionUID = 5576425603050236009L;

  PFUserDO user;

  List<HRViewUserEntryData> entries;

  long plannedSecondsSum = 0;

  long actualSecondsSum = 0;

  long plannedSecondsRestSum = 0;

  long actualSecondsRestSum = 0;

  boolean deleted;

  private HRPlanningDO hrPlanning;

  HRViewUserData(final PFUserDO user)
  {
    this.user = user;
    this.entries = new ArrayList<HRViewUserEntryData>();
  }

  void addTimesheet(final TimesheetDO timesheet)
  {
    addTimesheet(timesheet, (ProjektDO) null);
  }

  void addTimesheet(final TimesheetDO timesheet, final ProjektDO projekt)
  {
    ensureAndGetEntry(projekt).addTimesheet(timesheet);
    actualSecondsSum += (timesheet.getDuration() / 1000);
    if (projekt == null) {
      actualSecondsRestSum += (timesheet.getDuration() / 1000);
    }
  }

  void addTimesheet(final TimesheetDO timesheet, final KundeDO kunde)
  {
    ensureAndGetEntry(kunde).addTimesheet(timesheet);
    actualSecondsSum += (timesheet.getDuration() / 1000);
    if (kunde == null) {
      actualSecondsRestSum += (timesheet.getDuration() / 1000);
    }
  }

  void addPlanningEntry(final HRPlanningEntryDO entry)
  {
    addPlanningEntry(entry, (ProjektDO) null);
  }

  void addPlanningEntry(final HRPlanningEntryDO entry, final ProjektDO projekt)
  {
    ensureAndGetEntry(projekt).addPlanningEntry(entry);
    if (hrPlanning == null) {
      hrPlanning = entry.getPlanning();
    }
    plannedSecondsSum += entry.getTotalHours().multiply(NumberHelper.THREE_THOUSAND_SIX_HUNDRED).longValue();
    if (projekt == null) {
      plannedSecondsRestSum += entry.getTotalHours().multiply(NumberHelper.THREE_THOUSAND_SIX_HUNDRED).longValue();
    }
    if (entry.getPlanning() != null) {
      deleted = entry.getPlanning().isDeleted();
    }
  }

  void addPlanningEntry(final HRPlanningEntryDO entry, final KundeDO kunde)
  {
    ensureAndGetEntry(kunde).addPlanningEntry(entry);
    if (hrPlanning == null) {
      hrPlanning = entry.getPlanning();
    }
    plannedSecondsSum += entry.getTotalHours().multiply(NumberHelper.THREE_THOUSAND_SIX_HUNDRED).longValue();
    if (kunde == null) {
      plannedSecondsRestSum += entry.getTotalHours().multiply(NumberHelper.THREE_THOUSAND_SIX_HUNDRED).longValue();
    }
  }

  public Integer getPlanningId()
  {
    if (hrPlanning == null) {
      return null;
    }
    return hrPlanning.getId();
  }

  public boolean isDeleted()
  {
    return deleted;
  }

  public BigDecimal getPlannedDaysSum()
  {
    return new BigDecimal(plannedSecondsSum).divide(new BigDecimal(28800), 2, RoundingMode.HALF_UP);
  }

  public BigDecimal getActualDaysSum()
  {
    return new BigDecimal(actualSecondsSum).divide(new BigDecimal(28800), 2, RoundingMode.HALF_UP);
  }

  /**
   * @return The rest of days (not included in the child entries).
   */
  public BigDecimal getPlannedDaysRestSum()
  {
    return new BigDecimal(plannedSecondsRestSum).divide(new BigDecimal(28800), 2, RoundingMode.HALF_UP);
  }

  /**
   * @return The rest of days (not included in the child entries).
   */
  public BigDecimal getActualDaysRestSum()
  {
    return new BigDecimal(actualSecondsRestSum).divide(new BigDecimal(28800), 2, RoundingMode.HALF_UP);
  }

  public PFUserDO getUser()
  {
    return user;
  }

  public Integer getUserId()
  {
    if (user == null) {
      return null;
    }
    return user.getId();
  }

  public HRViewUserEntryData getEntry(final ProjektDO projekt)
  {
    for (final HRViewUserEntryData entry : entries) {
      if (projekt == null) {
        if (entry.projekt == null && entry.kunde == null) {
          return entry;
        }
      } else if (entry.projekt != null && entry.projekt.getId().equals(projekt.getId()) == true) {
        return entry;
      }
    }
    return null;
  }

  public HRViewUserEntryData getEntry(final KundeDO kunde)
  {
    for (final HRViewUserEntryData entry : entries) {
      if (kunde == null) {
        if (entry.kunde == null) {
          return entry;
        }
      } else if (entry.kunde != null && entry.kunde.getId().equals(kunde.getId()) == true) {
        return entry;
      }
    }
    return null;
  }

  private HRViewUserEntryData ensureAndGetEntry(final ProjektDO projekt)
  {
    HRViewUserEntryData entry = getEntry(projekt);
    if (entry == null) {
      entry = new HRViewUserEntryData(projekt);
      this.entries.add(entry);
    }
    return entry;
  }

  private HRViewUserEntryData ensureAndGetEntry(final KundeDO kunde)
  {
    HRViewUserEntryData entry = getEntry(kunde);
    if (entry == null) {
      entry = new HRViewUserEntryData(kunde);
      this.entries.add(entry);
    }
    return entry;
  }

  public int compareTo(final HRViewUserData o)
  {
    return this.user.getFullname().compareTo(o.user.getFullname());
  }

  /**
   * @see org.projectforge.framework.persistence.api.IdObject#getId()
   */
  @Override
  public Serializable getId()
  {
    return hrPlanning != null ? hrPlanning.getId() : null;
  }
}
