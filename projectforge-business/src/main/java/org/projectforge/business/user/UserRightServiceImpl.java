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

package org.projectforge.business.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.list.UnmodifiableList;
import org.jfree.util.Log;
import org.projectforge.business.fibu.AuftragRight;
import org.projectforge.business.fibu.ProjektRight;
import org.projectforge.business.gantt.GanttChartRight;
import org.projectforge.business.humanresources.HRPlanningRight;
import org.projectforge.business.meb.MebRight;
import org.projectforge.business.multitenancy.TenantChecker;
import org.projectforge.business.multitenancy.TenantRight;
import org.projectforge.business.teamcal.admin.right.TeamCalRight;
import org.projectforge.business.teamcal.event.right.TeamEventRight;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.IUserRightId;
import org.projectforge.framework.persistence.api.RightRightIdProviderService;
import org.projectforge.framework.persistence.api.UserRightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRightServiceImpl implements UserRightService, Serializable
{
  private static final long serialVersionUID = 7745893362798312310L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserRightServiceImpl.class);

  @Autowired
  AccessChecker accessChecker;

  @Autowired
  TenantChecker tenantChecker;

  /**
   * FALSE, TRUE;
   */
  public static final UserRightValue[] FALSE_TRUE = new UserRightValue[] { UserRightValue.FALSE, UserRightValue.TRUE };

  /**
   * FALSE, READONLY, READWRITE;
   */
  public static final UserRightValue[] FALSE_READONLY_READWRITE = new UserRightValue[] { UserRightValue.FALSE,
      UserRightValue.READONLY,
      UserRightValue.READWRITE };

  /**
   * FALSE, READONLY, PARTLY_READWRITE, READWRITE;
   */
  public static final UserRightValue[] FALSE_READONLY_PARTLYREADWRITE_READWRITE = new UserRightValue[] {
      UserRightValue.FALSE,
      UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE };

  /**
   * READONLY, READWRITE;
   */
  public static final UserRightValue[] READONLY_READWRITE = new UserRightValue[] { UserRightValue.READONLY,
      UserRightValue.READWRITE };

  /**
   * READONLY, PARTY_READWRITE, READWRITE;
   */
  public static final UserRightValue[] READONLY_PARTLYREADWRITE_READWRITE = new UserRightValue[] {
      UserRightValue.READONLY, UserRightValue.PARTLYREADWRITE, UserRightValue.READWRITE };

  public static final ProjectForgeGroup[] FIBU_GROUPS = { ProjectForgeGroup.FINANCE_GROUP,
      ProjectForgeGroup.CONTROLLING_GROUP };

  public static final ProjectForgeGroup[] FIBU_ORGA_GROUPS = { ProjectForgeGroup.FINANCE_GROUP,
      ProjectForgeGroup.ORGA_TEAM,
      ProjectForgeGroup.CONTROLLING_GROUP };

  public static final ProjectForgeGroup[] FIBU_ORGA_PM_GROUPS = { ProjectForgeGroup.FINANCE_GROUP,
      ProjectForgeGroup.ORGA_TEAM,
      ProjectForgeGroup.CONTROLLING_GROUP, ProjectForgeGroup.PROJECT_MANAGER, ProjectForgeGroup.PROJECT_ASSISTANT };

  private final Map<IUserRightId, UserRight> rights = new HashMap<IUserRightId, UserRight>();

  private final Map<String, IUserRightId> rightIds = new HashMap<String, IUserRightId>();

  private final List<UserRight> orderedRights = new ArrayList<UserRight>();
  /**
   * The user rights ids.
   */
  private Map<String, IUserRightId> userRightIds = new HashMap<>();

  static {

  }

  @Override
  public UserRight getRight(final IUserRightId id)
  {
    return rights.get(id);
  }

  @Override
  public UserRight getRight(String userRightId)
  {
    IUserRightId uid = userRightIds.get(userRightId);
    if (uid == null) {
      return null;
    }
    return rights.get(uid);
  }

  @Override
  public IUserRightId getRightId(final String userRightId) throws IllegalArgumentException
  {
    final IUserRightId rightId = rightIds.get(userRightId);
    if (rightId == null) {
      throw new IllegalArgumentException(
          "UserRightId with id '" + userRightId + "' not found (may-be not yet initialized).");
    }
    return rightId;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<UserRight> getOrderedRights()
  {
    return UnmodifiableList.decorate(orderedRights);
  }

  @PostConstruct
  public void init()
  {
    initUserRightIds();
    addRight(UserRightCategory.FIBU, UserRightId.FIBU_EMPLOYEE, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(UserRightCategory.FIBU, UserRightId.FIBU_EMPLOYEE_SALARY, FALSE_READONLY_READWRITE, FIBU_GROUPS)
        .setAvailableGroupRightValues(
            ProjectForgeGroup.CONTROLLING_GROUP, UserRightValue.FALSE, UserRightValue.READONLY);
    addRight(UserRightCategory.FIBU, UserRightId.FIBU_AUSGANGSRECHNUNGEN, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(UserRightCategory.FIBU, UserRightId.FIBU_EINGANGSRECHNUNGEN, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(UserRightCategory.FIBU, UserRightId.FIBU_DATEV_IMPORT, FALSE_TRUE, FIBU_GROUPS);
    addRight(UserRightCategory.FIBU, UserRightId.FIBU_COST_UNIT, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(UserRightCategory.FIBU, UserRightId.FIBU_ACCOUNTS, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(UserRightCategory.ORGA, UserRightId.ORGA_CONTRACTS, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(UserRightCategory.ORGA, UserRightId.ORGA_INCOMING_MAIL, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(UserRightCategory.ORGA, UserRightId.ORGA_OUTGOING_MAIL, FALSE_READONLY_READWRITE, FIBU_ORGA_GROUPS)
        .setReadOnlyForControlling();
    addRight(new TenantRight(accessChecker, tenantChecker));
    addRight(new ProjektRight(accessChecker));
    addRight(new AuftragRight(accessChecker));
    addRight(new MebRight(accessChecker));
    addRight(new GanttChartRight(accessChecker));
    addRight(new HRPlanningRight(accessChecker));
    addRight(new TeamCalRight(accessChecker));
    addRight(new TeamEventRight(accessChecker));
  }

  private void initUserRightIds()
  {

    ServiceLoader<RightRightIdProviderService> serviceLoader = ServiceLoader
        .load(RightRightIdProviderService.class);
    for (RightRightIdProviderService service : serviceLoader) {
      String cname = service.getClass().getName();
      for (IUserRightId uid : service.getUserRightIds()) {
        if (userRightIds.containsKey(uid.getId()) == true) {
          Log.error("Duplicated UserId: " + uid.getId());
        }
        userRightIds.put(uid.getId(), uid);
      }
    }

  }

  public UserGroupsRight addRight(final UserRightCategory category, final UserRightId id,
      final UserRightValue[] rightValues,
      final ProjectForgeGroup... fibuGroups)
  {
    final UserGroupsRight right = new UserGroupsRight(id, category, rightValues, fibuGroups);
    addRight(right);
    return right;
  }

  @Override
  public void addRight(final UserRight right)
  {
    final IUserRightId userRightId = right.getId();
    rights.put(right.getId(), right);
    rightIds.put(userRightId.getId(), userRightId);
    orderedRights.add(right);
  }
}
