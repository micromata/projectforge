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

import static org.junit.jupiter.api.Assertions.*;
import org.projectforge.business.ldap.LdapPerson;
import org.projectforge.business.ldap.LdapUser;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

public class LdapTestUtils
{
  public static void assertUser(final PFUserDO user, final String username, final String firstname,
      final String lastname,
      final String email, final String organization, final String description)
  {
    assertEquals(username, user.getUsername());
    assertEquals(firstname, user.getFirstname());
    assertEquals(lastname, user.getLastname());
    assertEquals(organization, user.getOrganization());
    assertEquals(email, user.getEmail());
    assertEquals(description, user.getDescription());
  }

  public static void assertUser(final LdapPerson user, final String uid, final String givenName, final String surname,
      final String mail[],
      final String organization, final String description)
  {
    assertEquals(uid, user.getUid());
    assertEquals(givenName, user.getGivenName());
    assertEquals(surname, user.getSurname());
    assertEquals(organization, user.getOrganization());
    assertArrayEquals(mail, user.getMail());
    assertEquals(description, user.getDescription());
  }

  public static LdapUser createLdapUser(final String username, final String firstname, final String lastname,
      final String email,
      final String organization, final String description)
  {
    return (LdapUser) new LdapUser().setUid(username).setGivenName(firstname).setSurname(lastname).setMail(email)
        .setOrganization(organization).setDescription(description);
  }

  public static void assertUser(final LdapUser user, final String username, final String firstname,
      final String lastname,
      final String email, final String organization, final String description)
  {
    assertEquals(username, user.getUid());
    assertEquals(firstname, user.getGivenName());
    assertEquals(lastname, user.getSurname());
    final String mail = user.getMail() != null && user.getMail().length > 0 ? user.getMail()[0] : null;
    assertEquals(email, mail);
    assertEquals(organization, user.getOrganization());
    assertEquals(description, user.getDescription());
  }

  public static void assertPosixAccountValues(final LdapUser ldapUser, final Integer uid, final Integer gid,
      final String homeDirectory,
      final String loginShell)
  {
    assertEquals(uid, ldapUser.getUidNumber());
    assertEquals(gid, ldapUser.getGidNumber());
    assertEquals(homeDirectory, ldapUser.getHomeDirectory());
    assertEquals(loginShell, ldapUser.getLoginShell());
  }

  public static void assertSambaAccountValues(final LdapUser ldapUser, final Integer sambaSIDNumber,
      final Integer sambaPrimaryGroupSIDNumber, final String sambaNTPassword)
  {
    assertEquals(sambaSIDNumber, ldapUser.getSambaSIDNumber());
    assertEquals(sambaPrimaryGroupSIDNumber, ldapUser.getSambaPrimaryGroupSIDNumber());
    if (sambaNTPassword == null) {
      assertNull(ldapUser.getSambaNTPassword());
    } else {
      assertEquals(sambaNTPassword, ldapUser.getSambaNTPassword());
    }
  }
}
