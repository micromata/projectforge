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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class PFUserDOConverterTest extends AbstractTestBase
{
  @Autowired
  LdapServiceImpl ldapService;

  @Autowired
  PFUserDOConverter pfUserDOConverter;

  @BeforeEach
  public void setup()
  {
    final LdapConfig ldapConfig = new LdapConfig();
    ldapService.setLdapConfig(ldapConfig);
    final LdapPosixAccountsConfig posixAccountsConfig = new LdapPosixAccountsConfig();
    ldapConfig.setPosixAccountsConfig(posixAccountsConfig);
  }

  @Test
  public void convert()
  {
    final Date now = new Date();
    PFUserDO user = new PFUserDO().setUsername("k.reinhard").setFirstname("Kai").setLastname("Reinhard")
        .setEmail("k.reinhard@micromata.de").setDescription("Developer").setOrganization("Micromata GmbH");
    user.setId(42);
    user.setLastWlanPasswordChange(now);

    LdapUser ldapUser = pfUserDOConverter.convert(user);
    assertEquals("k.reinhard", ldapUser.getUid());
    assertEquals("k.reinhard", ldapUser.getId());
    assertEquals(PFUserDOConverter.ID_PREFIX + "42", ldapUser.getEmployeeNumber());
    assertEquals("Kai Reinhard", ldapUser.getCommonName());
    assertEquals("Developer", ldapUser.getDescription());
    assertEquals("Kai", ldapUser.getGivenName());
    assertEquals("Reinhard", ldapUser.getSurname());
    assertEquals("Micromata GmbH", ldapUser.getOrganization());
    assertEquals(1, ldapUser.getMail().length);
    assertEquals("k.reinhard@micromata.de", ldapUser.getMail()[0]);
    assertEquals(now, ldapUser.getSambaPwdLastSet());

    user = pfUserDOConverter.convert(ldapUser);
    assertEquals("k.reinhard", user.getUsername());
    assertEquals(new Integer(42), user.getId());
    assertEquals("Developer", user.getDescription());
    assertEquals("Kai", user.getFirstname());
    assertEquals("Reinhard", user.getLastname());
    assertEquals("Micromata GmbH", user.getOrganization());
    assertEquals("k.reinhard@micromata.de", user.getEmail());
    assertEquals(now, user.getLastWlanPasswordChange());

    user = new PFUserDO();
    ldapUser = pfUserDOConverter.convert(user);
    assertNull(ldapUser.getId());
    assertNull(ldapUser.getUid());
    assertNull(ldapUser.getEmployeeNumber());
  }

  @Test
  public void copy()
  {
    final PFUserDO src = createUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    PFUserDO dest = createUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    assertFalse(PFUserDOConverter.copyUserFields(src, dest));
    assertUser(src, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    assertUser(dest, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    dest = new PFUserDO();
    assertTrue(PFUserDOConverter.copyUserFields(src, dest));
    assertUser(src, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    assertUser(dest, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    assertTrue(PFUserDOConverter.copyUserFields(src,
        createUser("", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer")));
    assertTrue(PFUserDOConverter.copyUserFields(src,
        createUser("kai", "", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer")));
    assertTrue(PFUserDOConverter.copyUserFields(src,
        createUser("kai", "Kai", "", "k.reinhard@acme.com", "Micromata", "Developer")));
    assertTrue(
        PFUserDOConverter.copyUserFields(src, createUser("kai", "Kai", "Reinhard", "", "Micromata", "Developer")));
    assertTrue(PFUserDOConverter.copyUserFields(src,
        createUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "", "Developer")));
    assertTrue(PFUserDOConverter.copyUserFields(src,
        createUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "")));
  }

  private PFUserDO createUser(final String username, final String firstname, final String lastname, final String email,
      final String organization, final String description)
  {
    return new PFUserDO().setUsername(username).setFirstname(firstname).setLastname(lastname).setEmail(email)
        .setOrganization(organization)
        .setDescription(description);
  }

  private void assertUser(final PFUserDO user, final String username, final String firstname, final String lastname,
      final String email,
      final String organization, final String description)
  {
    assertEquals(username, user.getUsername());
    assertEquals(firstname, user.getFirstname());
    assertEquals(lastname, user.getLastname());
    assertEquals(email, user.getEmail());
    assertEquals(organization, user.getOrganization());
    assertEquals(description, user.getDescription());
  }

  @Test
  public void copyLdapUser()
  {
    final LdapUser src = LdapTestUtils.createLdapUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata",
        "Developer");
    LdapUser dest = LdapTestUtils.createLdapUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata",
        "Developer");
    assertFalse(pfUserDOConverter.copyUserFields(src, dest));
    LdapTestUtils.assertUser(src, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    LdapTestUtils.assertUser(dest, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    dest = new LdapUser();
    assertTrue(pfUserDOConverter.copyUserFields(src, dest));
    LdapTestUtils.assertUser(src, "kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    LdapTestUtils.assertUser(dest, null, "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer");
    assertTrue(pfUserDOConverter.copyUserFields(src,
        LdapTestUtils.createLdapUser("kai", "", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer")));
    assertTrue(pfUserDOConverter.copyUserFields(src,
        LdapTestUtils.createLdapUser("kai", "", "Reinhard", "k.reinhard@acme.com", "Micromata", "Developer")));
    assertTrue(pfUserDOConverter.copyUserFields(src,
        LdapTestUtils.createLdapUser("kai", "Kai", "", "k.reinhard@acme.com", "Micromata", "Developer")));
    assertTrue(pfUserDOConverter.copyUserFields(src,
        LdapTestUtils.createLdapUser("kai", "Kai", "Reinhard", "", "Micromata", "Developer")));
    assertTrue(pfUserDOConverter.copyUserFields(src,
        LdapTestUtils.createLdapUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "", "Developer")));
    assertTrue(pfUserDOConverter.copyUserFields(src,
        LdapTestUtils.createLdapUser("kai", "Kai", "Reinhard", "k.reinhard@acme.com", "Micromata", "")));
  }

  @Test
  public void setNullMailArray()
  {
    final LdapUser ldapUser = new LdapUser();
    PFUserDOConverter.setMailNullArray(ldapUser);
    assertNull(ldapUser.getMail());
    ldapUser.setMail(new String[1]);
    PFUserDOConverter.setMailNullArray(ldapUser);
    assertNull(ldapUser.getMail());
    ldapUser.setMail(new String[2]);
    ldapUser.getMail()[1] = "Hurzel";
    assertEquals(ldapUser.getMail()[1], "Hurzel");
  }

  @Test
  public void testLdapValues()
  {
    PFUserDO user = new PFUserDO().setLdapValues("");
    user.setUsername("kai");
    LdapUser ldapUser = pfUserDOConverter.convert(user);
    LdapTestUtils.assertPosixAccountValues(ldapUser, null, null, null, null);
    user.setLdapValues("<values uidNumber=\"65535\" />");
    ldapUser = pfUserDOConverter.convert(user);
    LdapTestUtils.assertPosixAccountValues(ldapUser, 65535, -1, "/home/kai", "/bin/bash");
    ldapUser.setUidNumber(42).setGidNumber(1000).setHomeDirectory("/home/user").setLoginShell("/bin/ksh");
    user = pfUserDOConverter.convert(ldapUser);
    ldapUser = pfUserDOConverter.convert(user);
    LdapTestUtils.assertPosixAccountValues(ldapUser, 42, 1000, "/home/user", "/bin/ksh");
    assertEquals(
        "<values uidNumber=\"42\" gidNumber=\"1000\" homeDirectory=\"/home/user\" loginShell=\"/bin/ksh\"/>",
        user.getLdapValues());
  }
}
