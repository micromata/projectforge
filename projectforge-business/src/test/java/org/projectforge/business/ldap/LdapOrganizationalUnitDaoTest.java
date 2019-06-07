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
import org.junit.jupiter.api.Test;

@Disabled
public class LdapOrganizationalUnitDaoTest
{
  /*  private static final org.slf4j.Logger log = org.slf4j.Logger
      .getLogger(LdapOrganizationalUnitDaoTest.class);

  private LdapOrganizationalUnitDao ldapOrganizationalUnitDao;

  private LdapRealTestHelper ldapRealTestHelper;

  @BeforeMethod
  public void setup()
  {
    ldapRealTestHelper = new LdapRealTestHelper();
    ldapOrganizationalUnitDao = new LdapOrganizationalUnitDao();
    ldapOrganizationalUnitDao.setLdapConnector(ldapRealTestHelper.ldapConnector);
  }

  @Test
  public void deleteAndCreateOu()
  {
    if (ldapRealTestHelper.isAvailable() == false) {
      log.info("No LDAP server configured for tests. Skipping test.");
      return;
    }
    final String ou = "deactivated";
    final String path = "ou=pf-test-ou";
    ldapOrganizationalUnitDao.createIfNotExist(path, "description");
    Assertions.assertTrue(ldapOrganizationalUnitDao.doesExist(path));
    ldapOrganizationalUnitDao.createIfNotExist(ou, "description", path);
    Assertions.assertTrue(ldapOrganizationalUnitDao.doesExist(ou, path));

    ldapOrganizationalUnitDao.deleteIfExists(ou, path);
    Assertions.assertFalse(ldapOrganizationalUnitDao.doesExist(ou, path));
    ldapOrganizationalUnitDao.deleteIfExists(path);
    Assertions.assertFalse(ldapOrganizationalUnitDao.doesExist(path));
  }*/
}
