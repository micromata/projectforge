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

package org.projectforge.business.ldap;

import org.junit.jupiter.api.Disabled;
import org.projectforge.test.AbstractTestBase;
import org.junit.jupiter.api.Test;
@Disabled
public class LdapSlaveLoginHandlerTest extends AbstractTestBase
{/*
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LdapSlaveLoginHandlerTest.class);

  @Autowired
  LdapSlaveLoginHandler loginHandler;

  private LdapUserDao ldapUserDao;

  private LdapRealTestHelper ldapRealTestHelper;

  @BeforeMethod
  public void setup()
  {
    ldapRealTestHelper = new LdapRealTestHelper().setup();
    ldapUserDao = ldapRealTestHelper.ldapUserDao;
  }

  @AfterMethod
  public void tearDown()
  {
    ldapRealTestHelper.tearDown();
  }

  @Test
  public void testMockedSimpleMode()
  {
    final String userBase = "ou=pf-mock-test-users";
    final String testUsername = "mockedLdapSlaveTestuser";
    ldapUserDao = mock(LdapUserDao.class);
    when(ldapUserDao.authenticate(Mockito.eq(testUsername), Mockito.eq("successful"), Mockito.eq(userBase))).thenReturn(
        (LdapUser) new LdapUser().setUid(testUsername));
    when(ldapUserDao.authenticate(Mockito.anyString(), Mockito.eq("fail"), Mockito.eq(userBase))).thenReturn(null);
    loginHandler.ldapConfig = new LdapConfig().setUserBase(userBase);
    loginHandler.userDao = userDao;
    loginHandler.ldapUserDao = ldapUserDao;
    loginHandler.ldapOrganizationalUnitDao = mock(LdapOrganizationalUnitDao.class);
    loginHandler.initialize();
    loginHandler.setMode(LdapSlaveLoginHandler.Mode.SIMPLE);
    testSimpleMode(loginHandler, testUsername);
  }

  @Test
  public void testSimpleMode()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final LdapSlaveLoginHandler loginHandler = createLoginHandler();
    loginHandler.setMode(LdapSlaveLoginHandler.Mode.SIMPLE);
    final String testUsername = "ldapSlaveTestuser";
    final LdapUser ldapUser = (LdapUser) new LdapUser().setUid(testUsername).setGivenName("Kai").setSurname("Reinhard")
        .setEmployeeNumber("42");
    createLdapUser(ldapUser, "successful");
    testSimpleMode(loginHandler, testUsername);
    ldapUserDao.delete(ldapUser);
  }

  private LdapSlaveLoginHandler createLoginHandler()
  {
    loginHandler.ldapConfig = ldapRealTestHelper.ldapConfig;
    loginHandler.userDao = userDao;
    loginHandler.ldapUserDao = ldapUserDao;
    loginHandler.ldapOrganizationalUnitDao = ldapRealTestHelper.ldapOrganizationalUnitDao;
    loginHandler.initialize();
    Login.getInstance().setLoginHandler(loginHandler);
    return loginHandler;
  }

  private void createLdapUser(final LdapUser ldapUser, final String password)
  {
    final String userBase = ldapRealTestHelper.ldapConfig.getUserBase();
    ldapUser.setOrganizationalUnit(userBase);
    ldapUserDao.create(userBase, ldapUser);
    ldapUserDao.changePassword(ldapUser, null, password);
  }

  private void testSimpleMode(final LoginHandler loginHandler, final String testUsername)
  {
    logon(TEST_ADMIN_USER);
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    assertNull("If failed, a previous test run didn't cleared the data-base.",
        userGroupCache.getUser(testUsername));

    // Check failed login:
    LoginResult result = loginHandler.checkLogin(testUsername, "fail");
    assertEquals("User login failed against LDAP therefore login should be failed.", LoginResultStatus.FAILED,
        result.getLoginResultStatus());

    // Check successful login for new ProjectForge users:
    result = loginHandler.checkLogin(testUsername, "successful");
    assertEquals(LoginResultStatus.SUCCESS, result.getLoginResultStatus());
    assertNotNull("User should be returned.", result.getUser());
    PFUserDO user = userDao.getInternalByName(testUsername);
    assertNotNull("User should be created by login handler.", user);
    assertEquals(testUsername, user.getUsername());
    assertEquals(result.getUser().getId(), user.getId());

    // Check successful login for existing ProjectForge users:
    result = loginHandler.checkLogin(testUsername, "successful");
    assertEquals(LoginResultStatus.SUCCESS, result.getLoginResultStatus());
    assertNotNull("User should be returned.", result.getUser());
    user = userDao.getInternalByName(testUsername);
    assertNotNull("User should be created by login handler.", user);
    assertEquals(testUsername, user.getUsername());
    assertEquals(result.getUser().getId(), user.getId());

    // Check that LDAP is ignored for local users:
    user.setLocalUser(true);
    userDao.internalUpdate(user);
    result = loginHandler.checkLogin(testUsername, "successful");
    assertEquals("User is a local user, thus the LDAP authentication should be ignored.",
        LoginResultStatus.FAILED,
        result.getLoginResultStatus());
    userDao.createEncryptedPassword(user, "test");
    userDao.internalUpdate(user);
    result = loginHandler.checkLogin(testUsername, "test");
    assertEquals("User is a local user, thus authentication should be done by the login default handler.",
        LoginResultStatus.SUCCESS, result.getLoginResultStatus());
    user = result.getUser();
    assertEquals(testUsername, user.getUsername());
  }

  @Test
  public void loginInMockedSlaveMode()
  {
    final String userBase = "ou=pf-mock-test-users";
    final LdapUserDao ldapUserDao = mock(LdapUserDao.class);
    LoginResult loginResult;
    final LdapUser kai = (LdapUser) new LdapUser().setUid("kai").setDescription("Developer").setGivenName("Kai")
        .setMail("k.reinhard@acme.com").setOrganization("Micromata").setSurname("Reinhard");
    when(ldapUserDao.authenticate("kai", "successful", userBase)).thenReturn(kai);
    when(ldapUserDao.authenticate("kai", "fail", userBase)).thenReturn(null);
    when(ldapUserDao.findByUsername("kai", userBase)).thenReturn(kai);
    loginHandler.ldapUserDao = ldapUserDao;
    loginHandler.ldapConfig = new LdapConfig().setUserBase(userBase);
    loginHandler.userDao = userDao;
    assertEquals(LoginResultStatus.FAILED, loginHandler.checkLogin("kai", "fail").getLoginResultStatus());

    assertFalse("User shouldn't be available yet in the data-base.",
        userDao.doesUsernameAlreadyExist(new PFUserDO().setUsername("kai")));
    loginResult = loginHandler.checkLogin("kai", "successful");
    assertEquals(LoginResultStatus.SUCCESS, loginResult.getLoginResultStatus());
    LdapTestUtils.assertUser(loginResult.getUser(), "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata",
        "Developer");
    assertTrue("User should be created in data-base as a new user (in ldap).",
        userDao.doesUsernameAlreadyExist(new PFUserDO().setUsername("kai")));
    final PFUserDO user = userDao.getInternalByName("kai");
    LdapTestUtils.assertUser(user, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    assertEquals(userDao.checkPassword(user, "successful"), PasswordCheckResult.OK);

    userDao.internalMarkAsDeleted(user);
    assertEquals("User is deleted in data-base. Login not possible.", LoginResultStatus.LOGIN_EXPIRED,
        loginHandler.checkLogin("kai", "successful").getLoginResultStatus());
  }

  @Test
  public void testUserMode()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final LdapSlaveLoginHandler loginHandler = createLoginHandler();
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    loginHandler.setMode(LdapSlaveLoginHandler.Mode.USERS);
    final String testUsername1 = "ldapSlaveTestuserUserMode1";
    final String testUsername2 = "ldapSlaveTestuserUserMode2";
    final LdapUser ldapUser1 = (LdapUser) new LdapUser().setUid(testUsername1).setGivenName("Kai")
        .setSurname("Reinhard")
        .setEmployeeNumber("100");
    createLdapUser(ldapUser1, "successful");
    final LdapUser ldapUser2 = (LdapUser) new LdapUser().setUid(testUsername2).setGivenName("Kai")
        .setSurname("Reinhard")
        .setEmployeeNumber("101");
    createLdapUser(ldapUser2, "successful");

    logon(TEST_ADMIN_USER);
    assertNull("If failed, a previous test run didn't cleared the data-base.",
        userGroupCache.getUser(testUsername1));
    assertNull("If failed, a previous test run didn't cleared the data-base.",
        userGroupCache.getUser(testUsername2));

    // Check failed login:
    LoginResult result = loginHandler.checkLogin(testUsername1, "fail");
    assertEquals("User login failed against LDAP therefore login should be failed.", LoginResultStatus.FAILED,
        result.getLoginResultStatus());

    // Check successful login for new ProjectForge users:
    result = loginHandler.checkLogin(testUsername1, "successful");
    assertEquals(LoginResultStatus.SUCCESS, result.getLoginResultStatus());
    assertNotNull("User should be returned.", result.getUser());
    synchronizeLdapUsers(loginHandler);
    PFUserDO user = userDao.authenticateUser(testUsername1, "successful");
    assertNotNull("User should be created by login handler.", user);
    assertEquals(testUsername1, user.getUsername());
    result = loginHandler.checkLogin(testUsername1, "successful");
    assertEquals(result.getUser().getId(), user.getId());
    user = userDao.getInternalByName(testUsername2);
    result = loginHandler.checkLogin(testUsername2, "successful");
    assertNotNull("User should be created by login handler.", user);
    assertEquals(testUsername2, user.getUsername());
    assertEquals(result.getUser().getId(), user.getId());

    // Delete user2
    ldapUserDao.delete(ldapUser2);
    synchronizeLdapUsers(loginHandler);
    user = userDao.getInternalByName(testUsername2);
    assertTrue("User isn't available in LDAP, therefore should be deleted.", user.isDeleted());
    createLdapUser(ldapUser2, "successful");
    synchronizeLdapUsers(loginHandler);
    user = userDao.getInternalByName(testUsername2);
    assertFalse("User isn't available in LDAP, therefore should be deleted.", user.isDeleted());

    // Check that LDAP is ignored for local users:
    user.setLocalUser(true);
    userDao.createEncryptedPassword(user, "test");
    userDao.internalUpdate(user);
    result = loginHandler.checkLogin(testUsername2, "successful");
    assertEquals("User is a local user, thus the LDAP authentication should be ignored.",
        LoginResultStatus.FAILED,
        result.getLoginResultStatus());
    result = loginHandler.checkLogin(testUsername2, "test");
    assertEquals("User is a local user, thus the data-base authentication should be used.",
        LoginResultStatus.SUCCESS,
        result.getLoginResultStatus());

    // Delete all users
    ldapUserDao.delete(ldapUser1);
    ldapUserDao.delete(ldapUser2);
  }

  private void synchronizeLdapUsers(final LdapSlaveLoginHandler loginHandler)
  {
    final UserGroupCache userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
    userGroupCache.forceReload(); // Synchronize ldap users.
    while (true) {
      try {
        Thread.sleep(200);
      } catch (final InterruptedException ex) {
      }
      if (userGroupCache.isRefreshInProgress() == false && loginHandler.isRefreshInProgress() == false) {
        break;
      }
    }
  }*/
}
