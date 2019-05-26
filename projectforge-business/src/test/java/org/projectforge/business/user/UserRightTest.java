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

import org.junit.jupiter.api.Test;
import org.projectforge.business.multitenancy.TenantDao;
import org.projectforge.business.multitenancy.TenantRegistryMap;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.persistence.user.entities.UserRightDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserRightTest extends AbstractTestBase
{
  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserRightService userRights;

  @Autowired
  private UserRightDao userRightDao;

  @Autowired
  private TenantDao tenantDao;

  @Test
  public void testUserDO()
  {
    logon(AbstractTestBase.TEST_ADMIN_USER);
    PFUserDO user = new PFUserDO();
    user.setUsername("UserRightTest");
    user//
        .addRight(new UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.TRUE)) // Invalid setting / value!
        .addRight(new UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READONLY)) //
        .addRight(new UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.FALSE));

    final List<UserRightDO> userRights = new ArrayList<>(user.getRights());
    user.getRights().clear();
    Integer userId = userService.save(user);
    userRightDao.save(userRights);
    user = userService.internalGetById(userId);

    assignToDefaultTenant(user);
    Set<UserRightDO> rights = user.getRights();
    assertEquals( 3, rights.size(),"3 rights added to user");
    logon(user.getUsername());
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_AUSGANGSRECHNUNGEN, false, UserRightValue.TRUE),
            "User not in required groups.");
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, false, UserRightValue.READONLY),
            "User not in required groups.");
    try {
      accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, true, UserRightValue.READONLY);
      fail("AccessException required.");
    } catch (final AccessException ex) {
      // OK
    }
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.FALSE),
            "Right valid but not available (user is not in fibu group).");
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.TRUE),
            "Right valid but not available (user is not in fibu group).");
    logon(AbstractTestBase.TEST_ADMIN_USER);
    final GroupDO group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    user = userService.internalGetById(user.getId());
    rights = user.getRights();
    assertEquals( 3, rights.size(),
            "3 rights added to user");
    assertTrue(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_AUSGANGSRECHNUNGEN, false, UserRightValue.TRUE),
            "Invalid setting but value matches.");
    assertTrue(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, false, UserRightValue.READONLY),
            "Right matches.");
    assertTrue(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.FALSE),
            "Right valid.");
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.TRUE),
            "Right valid.");
    try {
      accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, true, UserRightValue.TRUE);
      fail("AccessException required.");
    } catch (final AccessException ex) {
      // OK
    }
  }

  private void assignToDefaultTenant(PFUserDO user)
  {
    Set<TenantDO> tenantsToAssign = new HashSet<>();
    tenantsToAssign.add(tenantDao.getDefaultTenant());
    tenantDao.internalAssignTenants(user, tenantsToAssign, null, false, false);
    TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache().forceReload();
  }

  @Test
  public void testControllingUserDO()
  {
    logon(AbstractTestBase.TEST_ADMIN_USER);
    PFUserDO user = new PFUserDO();
    user.setUsername("UserRightTestControlling");
    user//
        .addRight(new UserRightDO(UserRightId.ORGA_OUTGOING_MAIL, UserRightValue.FALSE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READONLY)) //
        .addRight(new UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE)) // Not available
        .addRight(new UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE));

    final List<UserRightDO> userRights = new ArrayList<>(user.getRights());
    user.getRights().clear();
    user = userService.internalGetById(userService.save(user));
    userRightDao.save(userRights);

    final GroupDO group = getGroup(ProjectForgeGroup.CONTROLLING_GROUP.toString());
    groupDao.assignGroups(user, Collections.singleton(group), null, false);

    assignToDefaultTenant(user);

    logon(user.getUsername());
    user = userService.internalGetById(user.getId());
    final Set<UserRightDO> rights = user.getRights();
    assertEquals( 4, rights.size(),"4 rights added to user");
    assertTrue(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_AUSGANGSRECHNUNGEN, false, UserRightValue.READONLY),"Right matches.");
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, false, UserRightValue.READWRITE),"Right matches but not available.");
    assertTrue(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.TRUE),"Right valid.");
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.FALSE),"Right valid.");
    try {
      accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, true, UserRightValue.READWRITE);
      fail("AccessException required.");
    } catch (final AccessException ex) {
      // OK
    }
    assertTrue(accessChecker.hasLoggedInUserReadAccess(UserRightId.FIBU_AUSGANGSRECHNUNGEN, false));
    assertTrue(accessChecker.hasLoggedInUserReadAccess(UserRightId.ORGA_INCOMING_MAIL, false)); // Because only one value is available and
    // therefore set
    // for controlling users.
    assertFalse(accessChecker.hasLoggedInUserReadAccess(UserRightId.ORGA_OUTGOING_MAIL, false)); // Also only one value is available but the
    // explicit
    // FALSE setting is taken.
  }

  @Test
  public void testConfigurable()
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final UserRight right = userRights.getRight(UserRightId.PM_HR_PLANNING);
    logon(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
    assertFalse(right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()),
            "Right is not configurable, because all available right values are automatically assigned to the current user");
    logon(AbstractTestBase.TEST_ADMIN_USER);
    assertFalse(right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()),
            "Right is not configurable, because no right values are available.");
    PFUserDO user = new PFUserDO();
    user.setUsername("testConfigurableRight");
    user = userService.internalGetById(userService.save(user));
    GroupDO group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertTrue(right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()),
            "Right is configurable, because serveral right values are available.");
    logon(AbstractTestBase.TEST_ADMIN_USER);
    group = getGroup(ProjectForgeGroup.PROJECT_MANAGER.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertFalse(right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()),
            "Right is not configurable, because all available right values are automatically assigned to the current user");
  }

  @Test
  public void testHRPlanningRight()
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final UserRight right = userRights.getRight(UserRightId.PM_HR_PLANNING);
    logon(AbstractTestBase.TEST_PROJECT_MANAGER_USER);
    assertTrue(accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE),
            "Right valid.");
    logon(AbstractTestBase.TEST_ADMIN_USER);
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE),
            "Right invalid.");
    assertFalse(right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()),
            "Right is not configurable, because no right values are available.");
    PFUserDO user = new PFUserDO();
    user.setUsername("testHRPlanningRight");
    user = userService.internalGetById(userService.save(user));
    assignToDefaultTenant(user);
    GroupDO group = getGroup(ProjectForgeGroup.CONTROLLING_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertFalse(accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE),
            "Right invalid.");
    assertTrue(right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()),
            "Right is configurable, because serveral right values are available.");
    logon(AbstractTestBase.TEST_ADMIN_USER);
    group = getGroup(ProjectForgeGroup.PROJECT_MANAGER.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertTrue(accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE),
            "Right now valid because project managers have always READWRITE access.");
    assertFalse(right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()),
            "Right is not configurable, because all available right values are automatically assigned to the current user");
  }
}
