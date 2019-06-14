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

import java.io.Serializable;

import org.projectforge.business.fibu.kost.Kost2ArtDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;

/**
 * Repräsentiert einen Eintrag innerhalb eines Wochenberichts eines Mitarbeiters zu einem Kostenträger (Anzahl Stunden).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MonthlyEmployeeReportEntry implements Serializable
{
  private static final long serialVersionUID = 7290000602224467755L;

  private Kost2DO kost2;

  private TaskDO task;

  private long millis;

  public MonthlyEmployeeReportEntry(Kost2DO kost2)
  {
    this.kost2 = kost2;
  }

  public MonthlyEmployeeReportEntry(TaskDO task)
  {
    this.task = task;
  }

  public void addMillis(long millis)
  {
    this.millis += millis;
  }

  /**
   * Only given, if task is not given and vice versa.
   */
  public Kost2DO getKost2()
  {
    return kost2;
  }

  /**
   * Only given, if kost2 is not given and vice versa.
   */
  public TaskDO getTask()
  {
    return task;
  }

  /**
   * If this entry has a kost2 with a working time fraction set or a kost2art with a working time fraction set then the fraction of millis
   * will be returned.
   */
  public long getWorkFractionMillis()
  {
    if (kost2 != null) {
      if (kost2.getWorkFraction() != null) {
        return (long) (kost2.getWorkFraction().doubleValue() * millis);
      }
      final Kost2ArtDO kost2Art = kost2.getKost2Art();
      if (kost2Art.getWorkFraction() != null) {
        return (long) (kost2Art.getWorkFraction().doubleValue() * millis);
      }
    }
    return this.millis;
  }

  public long getMillis()
  {
    return millis;
  }

  public String getFormattedDuration()
  {
    return MonthlyEmployeeReport.getFormattedDuration(millis);
  }
}
