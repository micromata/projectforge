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

package org.projectforge.business.ldap;

import org.junit.jupiter.api.Disabled;
import org.projectforge.test.AbstractTestBase;

@Disabled
public class LdapUserDaoTest extends AbstractTestBase
{/*
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LdapUserDaoTest.class);

  @Autowired
  private LdapUserDao ldapUserDao;

  @Autowired
  LdapService ldapService;

  @Autowired
  PFUserDOConverter pfUserDOConverter;

  private LdapRealTestHelper ldapRealTestHelper;

  private String getPath()
  {
    return ldapRealTestHelper.getUserPath();
  }

  @BeforeMethod
  public void setup()
  {
    ldapRealTestHelper = new LdapRealTestHelper().setup();
    if (ldapRealTestHelper.isAvailable() == false) {
      return;
    }
    ldapUserDao = ldapRealTestHelper.ldapUserDao;
    ldapService.getLdapConfig()
        .setPosixAccountsConfig(new LdapPosixAccountsConfig().setDefaultGidNumber(1000));
    ldapService.getLdapConfig()
        .setSambaAccountsConfig(new LdapSambaAccountsConfig().setSambaSIDPrefix("123-123-123"));
  }

  @AfterMethod
  public void tearDown()
  {
    ldapRealTestHelper.tearDown();
  }

  @Test
  public void createAuthenticateAndDeleteUser()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final String uid = "test-user-42";
    final LdapUser user = (LdapUser) new LdapUser().setUid(uid).setGivenName("Kai").setSurname("ProjectForge Test")
        .setDescription("description").setHomePhoneNumber("0123").setMail("kr@acme.com").setMobilePhoneNumber("4567")
        .setOrganization("ProjectForge").setTelephoneNumber("890").setEmployeeNumber("42");
    user.setOrganizationalUnit(getPath());
    ldapUserDao.createOrUpdate(getPath(), user);
    final LdapUser user2 = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(user2);
    LdapTestUtils.assertUser(user2, user.getUid(), user.getGivenName(), user.getSurname(), user.getMail(),
        user.getOrganization(),
        user.getDescription());
    assertEquals(LdapUtils.getOu(getPath()), LdapUtils.getOu(user2.getOrganizationalUnit()));

    assertNull(ldapUserDao.authenticate(uid, "", getPath()));
    // Change password
    ldapUserDao.changePassword(user, null, "hurzel");
    assertEquals(getPath(), ldapUserDao.findByUsername(uid, getPath()).getOrganizationalUnit());
    final LdapUser ldapUser = ldapUserDao.authenticate(uid, "hurzel", getPath());
    assertNotNull(ldapUser);
    assertEquals(user.getUid(), ldapUser.getUid());

    // Delete user
    ldapUserDao.delete(user);
    assertNull(ldapUserDao.findByUsername(uid, getPath()));
  }

  @Test
  public void activateAndDeactivateUser()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final String uid = "test-user-43";
    final LdapUser user = (LdapUser) new LdapUser().setUid(uid).setGivenName("Kai").setSurname("ProjectForge Test")
        .setEmployeeNumber("43");
    user.setOrganizationalUnit(getPath());
    ldapUserDao.createOrUpdate(getPath(), user);
    ldapUserDao.changePassword(user, null, "hurzel");
    final LdapUser ldapUser = ldapUserDao.authenticate(uid, "hurzel", getPath());
    assertNotNull(ldapUser);
    ldapUserDao.deactivateUser(user);
    assertNull(ldapUserDao.authenticate(uid, "hurzel", getPath()));
    final LdapUser user2 = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(user2);
    assertEquals(LdapUtils.getOu(LdapUserDao.DEACTIVATED_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(user2.getOrganizationalUnit()));

    // Reactivate user:
    ldapUserDao.reactivateUser(user2);
    assertNull(ldapUserDao.authenticate(uid, "hurzel", getPath()));
    // Delete user
    ldapUserDao.delete(user2);
    assertNull(ldapUserDao.findByUsername(uid, getPath()));
  }

  @Test
  public void updateUser()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final String uid = "test-user-44";
    final PFUserDO user = new PFUserDO().setUsername(uid).setLastname("Reinhard").setFirstname("Kai")
        .setOrganization("Micromata GmbH")
        .setEmail("k.reinhard@acme.com").setDeactivated(true);
    user.setId(44);
    // Test creation of deactivated user:
    final LdapUser ldapUser = pfUserDOConverter.convert(user);
    ldapUser.setOrganizationalUnit(getPath());
    ldapUserDao.createOrUpdate(getPath(), ldapUser);
    LdapUser ldapUser2 = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(ldapUser2);
    assertEquals(LdapUtils.getOu(LdapUserDao.DEACTIVATED_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(ldapUser2.getOrganizationalUnit()));
    assertTrue(ldapUser.isDeactivated());
    // Test update from deactivated to activated:
    ldapUser2.setDeactivated(false);
    ldapUserDao.update(getPath(), ldapUser2);
    ldapUser2 = ldapUserDao.findByUsername(uid, getPath());
    assertEquals(LdapUtils.getOu(getPath()), LdapUtils.getOu(ldapUser2.getOrganizationalUnit()));
    // Test update from activated to deactivated:
    ldapUser2.setDeactivated(true);
    ldapUserDao.update(getPath(), ldapUser2);
    ldapUser2 = ldapUserDao.findByUsername(uid, getPath());
    assertEquals(LdapUtils.getOu(LdapUserDao.DEACTIVATED_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(ldapUser2.getOrganizationalUnit()));
    assertTrue(ldapUser.isDeactivated());
    // Delete user
    ldapUserDao.delete(ldapUser2);
    assertNull(ldapUserDao.findByUsername(uid, getPath()));
  }

  @Test
  public void restrictedUsers()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final String uid = "test-user-45";
    final PFUserDO user = new PFUserDO().setUsername(uid).setLastname("Reinhard").setFirstname("Kai")
        .setOrganization("Micromata GmbH")
        .setEmail("k.reinhard@acme.com").setRestrictedUser(true);
    user.setId(45);
    // Test creation of restricted users:
    final LdapUser initialLdapUser = pfUserDOConverter.convert(user);
    initialLdapUser.setOrganizationalUnit(getPath());
    ldapUserDao.createOrUpdate(getPath(), initialLdapUser);
    LdapUser ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(ldapUser);
    assertEquals(LdapUtils.getOu(LdapUserDao.RESTRICTED_USER_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(ldapUser.getOrganizationalUnit()));
    assertTrue(ldapUser.isRestrictedUser());
    // Test update from restricted user to normal user:
    ldapUser.setRestrictedUser(false);
    ldapUserDao.update(getPath(), ldapUser);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertEquals(LdapUtils.getOu(getPath()), LdapUtils.getOu(ldapUser.getOrganizationalUnit()));
    // Test update from normal user to restricted user:
    ldapUser.setRestrictedUser(true);
    ldapUserDao.update(getPath(), ldapUser);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertEquals(LdapUtils.getOu(LdapUserDao.RESTRICTED_USER_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(ldapUser.getOrganizationalUnit()));
    assertTrue(ldapUser.isRestrictedUser());

    // Test deactivated users (restricted context should be ignored):
    ldapUser.setDeactivated(true);
    ldapUserDao.update(getPath(), ldapUser);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertEquals(LdapUtils.getOu(LdapUserDao.DEACTIVATED_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(ldapUser.getOrganizationalUnit()));
    assertTrue(ldapUser.isDeactivated());
    assertFalse(ldapUser.isRestrictedUser());

    // Delete user
    ldapUserDao.delete(ldapUser);
    assertNull(ldapUserDao.findByUsername(uid, getPath()));

    // Create restricted and deactivated user. Restriction should be ignored:
    ldapUser = pfUserDOConverter.convert(user);
    ldapUser.setDeactivated(true);
    ldapUser.setOrganizationalUnit(getPath());
    ldapUserDao.createOrUpdate(getPath(), ldapUser);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertEquals(LdapUtils.getOu(LdapUserDao.DEACTIVATED_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(ldapUser.getOrganizationalUnit()));
    assertTrue(ldapUser.isDeactivated());
    assertFalse(ldapUser.isRestrictedUser());

    ldapUser.setDeactivated(false).setRestrictedUser(true);
    ldapUserDao.createOrUpdate(getPath(), ldapUser);
    assertEquals(LdapUtils.getOu(LdapUserDao.RESTRICTED_USER_SUB_CONTEXT, getPath()),
        LdapUtils.getOu(ldapUser.getOrganizationalUnit()));
    assertTrue(ldapUser.isRestrictedUser());

    // Delete user
    ldapUserDao.delete(ldapUser);
    assertNull(ldapUserDao.findByUsername(uid, getPath()));
  }

  @Test
  public void posixAccountUsers()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    String uid = "test-user-46";
    PFUserDO user = new PFUserDO().setUsername(uid).setLastname("Reinhard").setFirstname("Kai")
        .setOrganization("Micromata GmbH")
        .setEmail("k.reinhard@acme.com");
    user.setId(46);
    final LdapUser initialLdapUser1 = pfUserDOConverter.convert(user);
    initialLdapUser1.setOrganizationalUnit(getPath());
    initialLdapUser1.setUidNumber(1042).setGidNumber(1000).setHomeDirectory("/home/kai").setLoginShell("/bin/bash");
    ldapUserDao.createOrUpdate(getPath(), initialLdapUser1);
    LdapUser ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(ldapUser);
    LdapTestUtils.assertPosixAccountValues(ldapUser, 1042, 1000, "/home/kai", "/bin/bash");

    uid = "test-user-47";
    user = new PFUserDO().setUsername(uid).setLastname("Reinhard").setFirstname("Kai").setOrganization("Micromata GmbH")
        .setEmail("k.reinhard@acme.com");
    user.setId(47);
    final LdapUser initialLdapUser2 = pfUserDOConverter.convert(user);
    initialLdapUser2.setOrganizationalUnit(getPath());
    ldapUserDao.createOrUpdate(getPath(), initialLdapUser2);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(ldapUser);
    LdapTestUtils.assertPosixAccountValues(ldapUser, null, null, null, null);
    ldapUser.setUidNumber(1047).setGidNumber(1000).setHomeDirectory("/home/kai").setLoginShell("/bin/bash");
    ldapUserDao.createOrUpdate(getPath(), ldapUser);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    LdapTestUtils.assertPosixAccountValues(ldapUser, 1047, 1000, "/home/kai", "/bin/bash");

    // Delete user
    ldapUserDao.delete(initialLdapUser1);
    ldapUserDao.delete(initialLdapUser2);
  }

  @Test
  public void sambaAccountUsers()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    String uid = "test-user-46";
    PFUserDO user = new PFUserDO().setUsername(uid).setLastname("Reinhard").setFirstname("Kai")
        .setOrganization("Micromata GmbH")
        .setEmail("k.reinhard@acme.com");
    user.setId(46);
    final LdapUser initialLdapUser1 = pfUserDOConverter.convert(user);
    initialLdapUser1.setOrganizationalUnit(getPath());
    String sambaNTPassword = SmbEncrypt.NTUNICODEHash("qwert123");
    initialLdapUser1.setSambaSIDNumber(1042).setSambaPrimaryGroupSIDNumber(1001).setSambaNTPassword(sambaNTPassword);
    ldapUserDao.createOrUpdate(getPath(), initialLdapUser1);
    LdapUser ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(ldapUser);
    LdapTestUtils.assertSambaAccountValues(ldapUser, 1042, 1001, null);
    ldapUserDao.changePassword(ldapUser, null, "qwert123");
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    LdapTestUtils.assertSambaAccountValues(ldapUser, 1042, 1001, sambaNTPassword);

    uid = "test-user-47";
    user = new PFUserDO().setUsername(uid).setLastname("Reinhard").setFirstname("Kai").setOrganization("Micromata GmbH")
        .setEmail("k.reinhard@acme.com");
    user.setId(47);
    final LdapUser initialLdapUser2 = pfUserDOConverter.convert(user);
    initialLdapUser2.setOrganizationalUnit(getPath());
    ldapUserDao.createOrUpdate(getPath(), initialLdapUser2);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    assertNotNull(ldapUser);
    LdapTestUtils.assertPosixAccountValues(ldapUser, null, null, null, null);
    sambaNTPassword = SmbEncrypt.NTUNICODEHash("hallo");
    ldapUser.setSambaSIDNumber(1047).setSambaPrimaryGroupSIDNumber(1001).setSambaNTPassword(sambaNTPassword);
    ldapUserDao.createOrUpdate(getPath(), ldapUser);
    ldapUser = ldapUserDao.findByUsername(uid, getPath());
    LdapTestUtils.assertSambaAccountValues(ldapUser, 1047, 1001, null);

    // Delete user
    ldapUserDao.delete(initialLdapUser1);
    ldapUserDao.delete(initialLdapUser2);

  }

  @Test
  public void testObjectClassesInitialization()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    ldapUserDao.initializeObjectClasses();
    assertArrayEquals(new String[] { "top", "inetOrgPerson" }, LdapUserDao.ALL_OBJECT_CLASSES);
    assertArrayEquals(new String[] { "top", "inetOrgPerson", "posixAccount" },
        LdapUserDao.ALL_OBJECT_CLASSES_WITH_POSIX_ACCOUNT);
    assertArrayEquals(new String[] { "top", "inetOrgPerson", "posixAccount", "sambaSamAccount" },
        LdapUserDao.ALL_OBJECT_CLASSES_WITH_SAMBA_AND_POSIX_ACCOUNT);
    assertArrayEquals(new String[] { "top", "inetOrgPerson", "sambaSamAccount" },
        LdapUserDao.ALL_OBJECT_CLASSES_WITH_SAMBA_ACCOUNT);
    String[] objectClasses = ldapUserDao.getAdditionalObjectClasses(new LdapUser());
    assertArrayEquals(LdapUserDao.ALL_OBJECT_CLASSES, objectClasses);
    objectClasses = ldapUserDao.getAdditionalObjectClasses(new LdapUser().setUidNumber(42));
    assertArrayEquals(LdapUserDao.ALL_OBJECT_CLASSES_WITH_POSIX_ACCOUNT, objectClasses);
    objectClasses = ldapUserDao.getAdditionalObjectClasses(new LdapUser().setSambaSIDNumber(42));
    assertArrayEquals(LdapUserDao.ALL_OBJECT_CLASSES_WITH_SAMBA_ACCOUNT, objectClasses);
    objectClasses = ldapUserDao.getAdditionalObjectClasses(new LdapUser().setUidNumber(42).setSambaSIDNumber(42));
    assertArrayEquals(LdapUserDao.ALL_OBJECT_CLASSES_WITH_SAMBA_AND_POSIX_ACCOUNT, objectClasses);
  }*/
}
