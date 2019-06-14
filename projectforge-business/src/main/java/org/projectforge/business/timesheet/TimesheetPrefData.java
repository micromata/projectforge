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

package org.projectforge.business.timesheet;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.utils.RecentQueue;

import java.util.Collection;
import java.util.List;


/**
 * Xstream support of timesheet user preferences.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("timesheetPref")
public class TimesheetPrefData
{
  public static final int MAX_RECENT = 50;

  RecentQueue<TimesheetPrefEntry> recents;

  RecentQueue<Integer> recentTasks;

  RecentQueue<String> recentLocations;

  public TimesheetPrefData()
  {
  }

  private synchronized void ensureRecents()
  {
    if (recents == null) {
      recents = new RecentQueue<TimesheetPrefEntry>(MAX_RECENT);
    }
  }

  private synchronized void ensureRecentTasks()
  {
    if (recentTasks == null) {
      recentTasks = new RecentQueue<Integer>(MAX_RECENT);
    }
  }

  private synchronized void ensureRecentLocations()
  {
    if (recentLocations == null) {
      recentLocations = new RecentQueue<String>(MAX_RECENT);
    }
  }

  /**
   * @return Recent entry at first position.
   */
  public TimesheetPrefEntry getRecentEntry() {
    return getRecentEntry(0);
  }

  public TimesheetPrefEntry getRecentEntry(Integer pos)
  {
    ensureRecents();
    return recents.get(pos);
  }

  public void appendRecentEntry(TimesheetPrefEntry entry)
  {
    ensureRecents();
    recents.setMaxSize(MAX_RECENT); // Needed, because size will be set via xstream deserialization.
    recents.append(entry);
  }

  public void appendRecentEntry(TimesheetDO entry)
  {
    TimesheetPrefEntry prefEntry = new TimesheetPrefEntry(entry);
    appendRecentEntry(prefEntry);
  }

  public List<TimesheetPrefEntry> getRecents()
  {
    if (recents == null) {
      return null;
    }
    return recents.getRecents();
  }

  public void setRecents(List<TimesheetPrefEntry> recents)
  {
    this.recents.setRecents(recents);
  }

  public Integer getRecentTask(Integer pos)
  {
    ensureRecentTasks();
    return recentTasks.get(pos);
  }

  public void appendRecentTask(Integer taskId)
  {
    if (taskId == null) {
      return;
    }
    ensureRecentTasks();
    recentTasks.setMaxSize(MAX_RECENT); // Needed, because max size will be set via xstream deserialization.
    recentTasks.append(taskId);
  }

  public void appendRecentLocation(String location)
  {
    if (StringUtils.isBlank(location) == true) {
      return;
    }
    ensureRecentLocations();
    recentLocations.setMaxSize(MAX_RECENT); // Needed, because max size will be set via xstream deserialization.
    recentLocations.append(location);
  }

  public List<Integer> getRecentTasks()
  {
    if (recentTasks == null) {
      return null;
    }
    return recentTasks.getRecents();
  }

  public void setRecentTasks(List<Integer> recentTasks)
  {
    this.recentTasks.setRecents(recentTasks);
  }

  public List<String> getRecentLocations()
  {
    if (recentLocations == null) {
      return null;
    }
    return recentLocations.getRecents();
  }

  public void setRecentLocations(RecentQueue<String> recentLocations)
  {
    this.recentLocations = recentLocations;
  }

  public void init(List<TimesheetDO> list)
  {
    if (CollectionUtils.isNotEmpty(list) == true) {
      ensureRecents();
      ensureRecentTasks();
      for (TimesheetDO timesheet : list) {
        TimesheetPrefEntry prefEntry = new TimesheetPrefEntry(timesheet);
        recents.addOnly(prefEntry);
        if (timesheet.getId() == null) {
          return;
        }
        recentTasks.addOnly(timesheet.getTaskId());
      }
    }
  }

  public void initLocations(Collection<String> locations)
  {
    if (CollectionUtils.isNotEmpty(locations) == true) {
      ensureRecentLocations();
      for (String location : locations) {
        if (StringUtils.isBlank(location) == true) {
          return;
        }
        recentLocations.addOnly(location);
      }
    }
  }
}
