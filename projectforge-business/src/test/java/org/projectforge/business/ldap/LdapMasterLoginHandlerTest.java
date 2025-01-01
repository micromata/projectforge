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

package org.projectforge.business.ldap;

import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.projectforge.business.login.Login;
import org.projectforge.business.login.LoginResultStatus;
import org.projectforge.business.user.GroupDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.business.test.AbstractTestBase;
import org.projectforge.test.JUnitLDAPTestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.naming.directory.SearchControls;
import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

// Create
// ~/ProjectForge/testldapConfig.xml
//<?xml version="1.0" encoding="UTF-8" ?>
//<ldapConfig>
//    <server>ldaps://192.168.76.177</server>
//    <port>636</port>
//    <userBase>ou=pf-test-users</userBase>
//    <groupBase>ou=pf-test-groups</groupBase>
//    <baseDN>dc=acme,dc=priv</baseDN>
//    <authentication>simple</authentication>
//    <managerUser>cn=manager</managerUser>
//    <managerPassword>test</managerPassword>
//    <sslCertificateFile>/Users/kai/ProjectForge/testldap.cert</sslCertificateFile>
//</ldapConfig>
public class LdapMasterLoginHandlerTest extends AbstractTestBase {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(LdapMasterLoginHandlerTest.class);

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private LdapMasterLoginHandler loginHandler;

  @Autowired
  private LdapUserDao ldapUserDao;

  @Autowired
  private LdapOrganizationalUnitDao ldapOrganizationalUnitDao;

  @Autowired
  private UserGroupCache userGroupCache;

  private LdapRealTestHelper ldapRealTestHelper;

  private JUnitLDAPTestWrapper jUnitLDAPTestWrapper;

  private String getPath() {
    return ldapRealTestHelper.getUserPath();
  }

  //@BeforeClass
  public void setUp() {
    //    try {
    //      Field developmentMode = GlobalConfiguration.class.getDeclaredField("developmentMode");
    //      developmentMode.setAccessible(true);
    //      developmentMode.setBoolean(GlobalConfiguration.getInstance(), false);
    //    } catch (NoSuchFieldException e) {
    //      e.printStackTrace();
    //    } catch (IllegalAccessException e) {
    //      e.printStackTrace();
    //    }
    //    Thread thread = new Thread(() -> {
    //      Result result = JUnitCore.runClasses(JUnitLDAPTestWrapper.class);
    //      if (result.wasSuccessful() == false) {
    //        Assertions.fail();
    //      }
    //    });

    //    thread.run();
  }

  //@BeforeMethod
  public void setup() {
    // TODO repair init
    LdapServer ldapServer = JUnitLDAPTestWrapper.ldapServerWrap;
    SchemaManager schemaManager = ldapServer.getDirectoryService().getSchemaManager();
    JdbmPartition jdbmPartition = new JdbmPartition();
    jdbmPartition.setPartitionDir(new File("."));
    DN dnMicromata = null;
    try {
      dnMicromata = new DN("dc=example,dc=org");
      jdbmPartition.setSuffix(String.valueOf(dnMicromata));
    } catch (LdapInvalidDnException e) {
      e.printStackTrace();
    }
    try {
      ldapServer.getDirectoryService().addPartition(jdbmPartition);
    } catch (Exception e) {
      e.printStackTrace();
    }

    jdbmPartition = new JdbmPartition();
    jdbmPartition.setPartitionDir(new File("."));
    DN dnUsers = null;
    try {
      dnUsers = new DN("ou=users");
      dnUsers.addAll(dnMicromata);
    } catch (LdapInvalidDnException e) {
      e.printStackTrace();
    }
    try {
      jdbmPartition.setSuffix(String.valueOf(dnUsers));
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      ldapServer.getDirectoryService().addPartition(jdbmPartition);
    } catch (Exception e) {
      e.printStackTrace();
    }
    jdbmPartition = new JdbmPartition();
    jdbmPartition.setPartitionDir(new File("."));
    DN dnDeactivated = null;
    try {
      dnDeactivated = new DN("ou=deactivated");
      dnDeactivated.addAll(dnUsers);
      jdbmPartition.setSuffix(String.valueOf(dnDeactivated));
    } catch (LdapInvalidDnException e) {
      e.printStackTrace();
    }

    jdbmPartition = new JdbmPartition();
    jdbmPartition.setPartitionDir(new File("."));
    DN dnRestricted = null;
    try {
      dnRestricted = new DN("ou=restricted");
      dnRestricted.addAll(dnUsers);
      jdbmPartition.setSuffix(String.valueOf(dnRestricted));
    } catch (LdapInvalidDnException e) {
      e.printStackTrace();
    }
    ldapRealTestHelper = new LdapRealTestHelper().setup();
    ldapUserDao = ldapRealTestHelper.ldapUserDao;

  }

  //@Test
  public void testSearchAllAttrs() throws Exception {
    //LdapContext ctx = (LdapContext) getWiredContext( JUnitLDAPTestWrapper.ldapServerWrap , null ).lookup( "ou=system" );

    SearchControls controls = new SearchControls();
    controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
    controls.setReturningAttributes(new String[]{"+", "*"});

    //NamingEnumeration<SearchResult> res = ctx.search("", "(ObjectClass=*)", controls);

    //    assertTrue(res.hasMore());
    //
    //    while (res.hasMoreElements()) {
    //      SearchResult result = (SearchResult) res.next();
    //
    //      System.out.println(result.getName());
    //    }
  }

  //@Test
  public void loginAndCreateLdapUser() {
    final String userBase = "ou=pf-mock-test-users";
    final LdapUserDao ldapUserDao = mock(LdapUserDao.class);
    loginHandler.ldapConfig = new LdapConfig().setUserBase(userBase);
    loginHandler.ldapUserDao = ldapUserDao;
    loginHandler.ldapOrganizationalUnitDao = mock(LdapOrganizationalUnitDao.class);
    loginHandler.initialize();
    Login.getInstance().setLoginHandler(loginHandler);
    logon(AbstractTestBase.TEST_ADMIN_USER);
    final PFUserDO user = new PFUserDO();
    user.setUsername("kai");
    user.setFirstname("Kai");
    user.setLastname("Reinhard");
    userService.encryptAndSavePassword(user, "successful".toCharArray());
    userService.insert(user, false);
    Assertions.assertEquals(LoginResultStatus.SUCCESS, loginHandler.checkLogin("kai", "successful".toCharArray()).getLoginResultStatus());

    final ArgumentCaptor<LdapUser> argumentCaptor = ArgumentCaptor.forClass(LdapUser.class);
    verify(ldapUserDao).createOrUpdate(Mockito.anyString(), argumentCaptor.capture());
    final LdapUser createdLdapUser = argumentCaptor.getValue();
    Assertions.assertEquals("kai", createdLdapUser.getUid());
    Assertions.assertEquals("Kai", createdLdapUser.getGivenName());
    Assertions.assertEquals("Reinhard", createdLdapUser.getSurname());
    // Assertions.assertEquals("successful", createdLdapUser.get());
    logoff();
  }

  //@Test
  public void realTest() {

    if (!ldapRealTestHelper.isAvailable()) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    } /*
     * final String userBase = "ou=users"; LdapConfig ldapConfig = new LdapConfig(); ldapConfig.setPort(1024);
     * ldapConfig.setBaseDN("dc=example,dc=org"); ldapConfig.setGroupBase("ou=users dc=example,dc=org");
     * ldapConfig.setManagerUser("uid=admin,ou=system"); ldapConfig.setServer("localhost");
     * ldapConfig.setManagerPassword("secret");
     *
     * ldapOrganizationalUnitDao.setLdapConnector(new LdapConnector(ldapConfig.setUserBase(userBase)));
     * logon(TEST_ADMIN_USER); loginHandler.ldapConfig = ldapConfig.setUserBase("ou=users"); loginHandler.userDao =
     * userDao; loginHandler.ldapUserDao = ldapUserDao; loginHandler.ldapOrganizationalUnitDao =
     * ldapOrganizationalUnitDao; loginHandler.initialize(); final LdapMasterLoginHandler loginHandler =
     * this.loginHandler; // Create users and group. final Integer userId1 = createUser("ldapMaster1", "test123",
     * "firstname1", "lastname1"); final Integer userId2 = createUser("ldapMaster2", "test123", "firstname2",
     * "lastname2"); final Integer userId3 = createUser("ldapMaster3", "test123", "firstname3", "lastname3"); final
     * Integer userId4 = createUser("ldapMaster4", "test123", "firstname4", "lastname4"); final Integer groupId1 =
     * createGroup("ldapMasterGroup1", "This is a stupid description."); GroupDO group =
     * groupDao.internalGetById(groupId1); synchronizeLdapUsers(loginHandler); LdapGroup ldapGroup =
     * ldapGroupDao.findById(groupId1); assertTrue(isMembersEmpty(ldapGroup));
     *
     * // Assign users to group group.setAssignedUsers(new HashSet<PFUserDO>()); final UserGroupCache userGroupCache =
     * TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache();
     * group.addUser(userGroupCache.getUser(userId1)); group.addUser(userGroupCache.getUser(userId2));
     * group.addUser(userGroupCache.getUser(userId3)); groupDao.internalUpdate(group);
     * synchronizeLdapUsers(loginHandler); ldapGroup = ldapGroupDao.findById(groupId1); assertMembers(ldapGroup,
     * "ldapMaster1", "ldapMaster2", "ldapMaster3"); Assertions.assertFalse(isMembersEmpty(ldapGroup)); LdapUser ldapUser
     * = ldapUserDao.findById(userId1, getPath()); Assertions.assertEquals("ldapMaster1", ldapUser.getUid());
     *
     * // Renaming one user, deleting one user and assigning third user
     * userDao.internalMarkAsDeleted(userDao.getById(userId2)); PFUserDO user3 = userDao.getById(userId3);
     * user3.setUsername("ldapMasterRenamed3"); userDao.internalUpdate(user3); group =
     * userGroupCache.getGroup(groupId1); group.addUser(userDao.getById(userId4)); groupDao.internalUpdate(group);
     * synchronizeLdapUsers(loginHandler); ldapGroup = ldapGroupDao.findById(groupId1); assertMembers(ldapGroup,
     * "ldapMaster1", "ldapMasterRenamed3", "ldapMaster4");
     *
     * // Renaming one user and mark him as restricted user3 = userDao.getById(userId3);
     * user3.setUsername("ldapMaster3"); user3.setRestrictedUser(true); userDao.internalUpdate(user3);
     * synchronizeLdapUsers(loginHandler); ldapUser = ldapUserDao.findById(userId3, getPath());
     * Assertions.assertEquals("ldapMaster3", ldapUser.getUid());
     * assertTrue(ldapUser.getOrganizationalUnit().contains("ou=restricted")); ldapGroup =
     * ldapGroupDao.findById(groupId1); assertMembers(ldapGroup, "ldapMaster1", "ldapMaster3,ou=restricted",
     * "ldapMaster4");
     *
     * // Renaming group group = groupDao.getById(groupId1); group.setName("ldapMasterGroupRenamed1");
     * groupDao.internalUpdate(group); synchronizeLdapUsers(loginHandler); ldapGroup =
     * ldapGroupDao.findById(groupId1); assertMembers(ldapGroup, "ldapMaster1", "ldapMaster3,ou=restricted",
     * "ldapMaster4"); Assertions.assertEquals("ldapMasterGroupRenamed1", ldapGroup.getCommonName());
     *
     * // Change password // /* final PFUserDO user1 = userDao.getById(userId1); final LoginResult loginResult =
     * loginService.checkLogin(user1.getUsername(), "test123"); // Assertions.assertEquals(LoginResultStatus.SUCCESS,
     * loginResult.getLoginResultStatus()); // Assertions.assertNotNull(ldapUserDao.authenticate(user1.getUsername(),
     * "test123")); Login.getInstance().passwordChanged(user1, "newpassword"); //
     * Assertions.assertNotNull(ldapUserDao.authenticate(user1.getUsername(), "newpassword"));
     *//*
     * // Delete all groups final Collection<GroupDO> groups = userGroupCache.getAllGroups(); for (final GroupDO g :
     * groups) { groupDao.internalMarkAsDeleted(g); } synchronizeLdapUsers(loginHandler);
     * Assertions.assertEquals(0,ldapGroupDao.findAll(ldapRealTestHelper.ldapConfig.getGroupBase()).size(),
     * "LDAP groups must be empty (all groups are deleted in the PF data-base)."); final Collection<PFUserDO> users
     * = userGroupCache.getAllUsers(); for (final PFUserDO user : users) { userDao.internalMarkAsDeleted(user); }
     * synchronizeLdapUsers(loginHandler); Assertions.assertEquals(
     * 0,ldapUserDao.findAll(ldapRealTestHelper.ldapConfig.getGroupBase()).size(),
     * "LDAP users must be empty (all user are deleted in the PF data-base)."); ldapUser =
     * ldapUserDao.findById(userId1, getPath()); Assertions.assertNull(ldapUser);
     */
  }

  private boolean isMembersEmpty(final LdapGroup ldapGroup) {
    final Set<String> members = ldapGroup.getMembers();
    if (CollectionUtils.isEmpty(members)) {
      return true;
    }
    if (members.size() > 1) {
      return false;
    }
    final String member = members.iterator().next();
    return member == null || member.startsWith("cn=none");
  }

  private void assertMembers(final LdapGroup ldapGroup, final String... usernames) {
    final Set<String> members = ldapGroup.getMembers();
    Assertions.assertFalse(CollectionUtils.isEmpty(members));
    Assertions.assertEquals(usernames.length, members.size());
    final LdapConfig ldapConfig = ldapRealTestHelper.ldapConfig;
    for (final String username : usernames) {
      final String user = "uid=" + username + "," + ldapConfig.getUserBase() + "," + ldapConfig.getBaseDN();
      assertTrue(members.contains(user));
    }
  }

  private Long createUser(final String username, final char[] password, final String firstname,
                             final String lastname) {
    final PFUserDO user = new PFUserDO();
    user.setUsername(username);
    user.setFirstname(firstname);
    user.setLastname(lastname);
    userService.encryptAndSavePassword(user, password);
    return (Long) userService.insert(user, false);
  }

  private Long createGroup(final String name, final String description) {
    final GroupDO group = new GroupDO();
    group.setName(name);
    group.setDescription(description);
    return (Long) groupDao.insert(group, false);
  }

  private LdapMasterLoginHandler createLoginHandler() {
    loginHandler.ldapConfig = ldapRealTestHelper.ldapConfig;
    loginHandler.ldapUserDao = ldapUserDao;
    loginHandler.ldapOrganizationalUnitDao = ldapRealTestHelper.ldapOrganizationalUnitDao;
    loginHandler.initialize();
    Login.getInstance().setLoginHandler(loginHandler);
    return loginHandler;
  }

  private void synchronizeLdapUsers(final LdapMasterLoginHandler loginHandler) {
    userGroupCache.forceReload(); // Synchronize ldap users.
    while (true) {
      try {
        Thread.sleep(200);
      } catch (final InterruptedException ex) {
      }
      if (!userGroupCache.isRefreshInProgress() && !loginHandler.isRefreshInProgress()) {
        break;
      }
    }
  }

  /**
   * @param groupDao the groupDao to set
   * @return this for chaining.
   */
  public void setGroupDao(final GroupDao groupDao) {
    this.groupDao = groupDao;
  }

  public JUnitLDAPTestWrapper getjUnitLDAPTestWrapper() {
    return jUnitLDAPTestWrapper;
  }

  public void setjUnitLDAPTestWrapper(JUnitLDAPTestWrapper jUnitLDAPTestWrapper) {
    this.jUnitLDAPTestWrapper = jUnitLDAPTestWrapper;
  }
}
