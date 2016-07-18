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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.util.HashSet;
import java.util.Set;

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
import org.testng.annotations.Test;

public class UserRightTest extends AbstractTestBase
{
  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserRightService userRights;

  @Autowired
  private TenantDao tenantDao;

  @Test
  public void testUserDO()
  {
    logon(TEST_ADMIN_USER);
    PFUserDO user = new PFUserDO();
    user.setUsername("UserRightTest");
    user//
        .addRight(new UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.TRUE)) // Invalid setting / value!
        .addRight(new UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READONLY)) //
        .addRight(new UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.FALSE));
    user = userService.getById(userService.save(user));
    assignToDefaultTenant(user);
    Set<UserRightDO> rights = user.getRights();
    assertEquals("3 rights added to user", 3, rights.size());
    logon(user.getUsername());
    assertFalse("User not in required groups.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_AUSGANGSRECHNUNGEN, false, UserRightValue.TRUE));
    assertFalse("User not in required groups.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, false, UserRightValue.READONLY));
    try {
      accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, true, UserRightValue.READONLY);
      fail("AccessException required.");
    } catch (final AccessException ex) {
      // OK
    }
    assertFalse("Right valid but not available (user is not in fibu group).",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.FALSE));
    assertFalse("Right valid but not available (user is not in fibu group).",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.TRUE));
    logon(TEST_ADMIN_USER);
    final GroupDO group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    user = userService.getById(user.getId());
    rights = user.getRights();
    assertEquals("3 rights added to user", 3, rights.size());
    assertTrue("Invalid setting but value matches.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_AUSGANGSRECHNUNGEN, false, UserRightValue.TRUE));
    assertTrue("Right matches.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, false, UserRightValue.READONLY));
    assertTrue("Right valid.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.FALSE));
    assertFalse("Right valid.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.TRUE));
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
    logon(TEST_ADMIN_USER);
    PFUserDO user = new PFUserDO();
    user.setUsername("UserRightTestControlling");
    user//
        .addRight(new UserRightDO(UserRightId.ORGA_OUTGOING_MAIL, UserRightValue.FALSE)) //
        .addRight(new UserRightDO(UserRightId.FIBU_AUSGANGSRECHNUNGEN, UserRightValue.READONLY)) //
        .addRight(new UserRightDO(UserRightId.FIBU_EINGANGSRECHNUNGEN, UserRightValue.READWRITE)) // Not available
        .addRight(new UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE));
    user = userService.getById(userService.save(user));
    assignToDefaultTenant(user);
    final GroupDO group = getGroup(ProjectForgeGroup.CONTROLLING_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    user = userService.getById(user.getId());
    final Set<UserRightDO> rights = user.getRights();
    assertEquals("4 rights added to user", 4, rights.size());
    assertTrue("Right matches.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_AUSGANGSRECHNUNGEN, false, UserRightValue.READONLY));
    assertFalse("Right matches but not available.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_EINGANGSRECHNUNGEN, false, UserRightValue.READWRITE));
    assertTrue("Right valid.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.TRUE));
    assertFalse("Right valid.",
        accessChecker.hasLoggedInUserRight(UserRightId.FIBU_DATEV_IMPORT, false, UserRightValue.FALSE));
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
    logon(TEST_PROJECT_MANAGER_USER);
    assertFalse(
        "Right is not configurable, because all available right values are automatically assigned to the current user",
        right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()));
    logon(TEST_ADMIN_USER);
    assertFalse("Right is not configurable, because no right values are available.",
        right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()));
    PFUserDO user = new PFUserDO();
    user.setUsername("testConfigurableRight");
    user = userService.getById(userService.save(user));
    GroupDO group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertTrue("Right is configurable, because serveral right values are available.",
        right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()));
    logon(TEST_ADMIN_USER);
    group = getGroup(ProjectForgeGroup.PROJECT_MANAGER.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertFalse(
        "Right is not configurable, because all available right values are automatically assigned to the current user",
        right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()));
  }

  @Test
  public void testHRPlanningRight()
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    final UserRight right = userRights.getRight(UserRightId.PM_HR_PLANNING);
    logon(TEST_PROJECT_MANAGER_USER);
    assertTrue("Right valid.",
        accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE));
    logon(TEST_ADMIN_USER);
    assertFalse("Right invalid.",
        accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE));
    assertFalse("Right is not configurable, because no right values are available.",
        right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()));
    PFUserDO user = new PFUserDO();
    user.setUsername("testHRPlanningRight");
    user = userService.getById(userService.save(user));
    assignToDefaultTenant(user);
    GroupDO group = getGroup(ProjectForgeGroup.CONTROLLING_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    group = getGroup(ProjectForgeGroup.FINANCE_GROUP.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertFalse("Right invalid.",
        accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE));
    assertTrue("Right is configurable, because serveral right values are available.",
        right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()));
    logon(TEST_ADMIN_USER);
    group = getGroup(ProjectForgeGroup.PROJECT_MANAGER.toString());
    group.getAssignedUsers().add(user);
    groupDao.update(group);
    logon(user.getUsername());
    assertTrue("Right now valid because project managers have always READWRITE access.",
        accessChecker.hasLoggedInUserRight(UserRightId.PM_HR_PLANNING, false, UserRightValue.READWRITE));
    assertFalse(
        "Right is not configurable, because all available right values are automatically assigned to the current user",
        right.isConfigurable(userGroupCache, ThreadLocalUserContext.getUser()));
  }
}
