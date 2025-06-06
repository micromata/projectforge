/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.hibernate.Hibernate;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.io.Serializable;
import java.util.*;


/**
 * Is not synchronized.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class HRViewData implements Serializable
{
  private static final long serialVersionUID = 8940066588727279286L;

  HRFilter filter;

  Map<Long, HRViewUserData> userDatas;

  private Map<Long, ProjektDO> projects;

  private Map<Long, KundeDO> customers;

  private List<HRViewUserData> sortedUserDatas;

  private List<ProjektDO> sortedProjects;

  private List<KundeDO> sortedCustomers;

  HRViewData(final HRFilter filter)
  {
    this.filter = filter;
    userDatas = new HashMap<>();
    projects = new HashMap<>();
    customers = new HashMap<>();
  }

  void addTimesheet(final TimesheetDO sheet, final PFUserDO user)
  {
    ensureAndGetUserData(user).addTimesheet(sheet);
    sortedUserDatas = null;
  }

  void addTimesheet(final TimesheetDO sheet, final PFUserDO user, final ProjektDO projekt)
  {
    ensureAndGetUserData(user).addTimesheet(sheet, projekt);
    addProjekt(projekt);
    sortedUserDatas = null;
  }

  void addTimesheet(final TimesheetDO sheet, final PFUserDO user, final KundeDO kunde)
  {
    ensureAndGetUserData(user).addTimesheet(sheet, kunde);
    addKunde(kunde);
    sortedUserDatas = null;
  }

  void addHRPlanningEntry(final HRPlanningEntryDO entry, final PFUserDO user)
  {
    ensureAndGetUserData(user).addPlanningEntry(entry);
    sortedUserDatas = null;
  }

  void addHRPlanningEntry(final HRPlanningEntryDO entry, final PFUserDO user, final ProjektDO projekt)
  {
    ensureAndGetUserData(user).addPlanningEntry(entry, projekt);
    addProjekt(projekt);
    sortedUserDatas = null;
  }

  void addHRPlanningEntry(final HRPlanningEntryDO entry, final PFUserDO user, final KundeDO kunde)
  {
    ensureAndGetUserData(user).addPlanningEntry(entry, kunde);
    addKunde(kunde);
    sortedUserDatas = null;
  }

  private void addProjekt(final ProjektDO projekt)
  {
    if (projekt != null) {
      Hibernate.initialize(projekt);
      if (!projects.containsKey(projekt.getId())) {
        projects.put(projekt.getId(), projekt);
        sortedProjects = null;
      }
    }
  }

  private void addKunde(final KundeDO kunde)
  {
    if (kunde != null) {
      Hibernate.initialize(kunde);
      if (!customers.containsKey(kunde.getNummer())) {
        customers.put(kunde.getNummer(), kunde);
        sortedCustomers = null;
      }
    }
  }

  public List<ProjektDO> getProjects()
  {
    if (sortedProjects == null) {
      sortedProjects = new ArrayList<>();
      sortedProjects.addAll(projects.values());
      sortedProjects.sort(new Comparator<ProjektDO>() {
        public int compare(ProjektDO o1, ProjektDO o2) {
          return StringHelper.compareTo(o1.getProjektIdentifierDisplayName(), o2.getProjektIdentifierDisplayName());
        }
      });
    }
    return sortedProjects;
  }

  public List<KundeDO> getCustomers()
  {
    if (sortedCustomers == null) {
      sortedCustomers = new ArrayList<>();
      sortedCustomers.addAll(customers.values());
      sortedCustomers.sort(new Comparator<KundeDO>() {
        public int compare(KundeDO o1, KundeDO o2) {
          return StringHelper.compareTo(o1.getKundeIdentifierDisplayName(), o2.getKundeIdentifierDisplayName());
        }
      });
    }
    return sortedCustomers;
  }

  public List<HRViewUserData> getUserDatas()
  {
    if (sortedUserDatas == null) {
      sortedUserDatas = new ArrayList<>();
      sortedUserDatas.addAll(userDatas.values());
      sortedUserDatas.sort(new Comparator<HRViewUserData>() {
        public int compare(HRViewUserData o1, HRViewUserData o2) {
          return StringHelper.compareTo(o1.getUser().getFullname(), o2.getUser().getFullname());
        }
      });
    }
    return sortedUserDatas;
  }

  public boolean containsUser(final PFUserDO user)
  {
    return userDatas.containsKey(user.getId());
  }

  public HRViewUserData getUserData(final PFUserDO user)
  {
    return userDatas.get(user.getId());
  }

  HRViewUserData ensureAndGetUserData(final PFUserDO user)
  {
    HRViewUserData data = userDatas.get(user.getId());
    if (data == null) {
      data = new HRViewUserData(user);
      userDatas.put(user.getId(), data);
    }
    return data;
  }
}
