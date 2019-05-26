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
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.*;

 class PFUserDOTest extends AbstractTestBase
{
  @Test
   void testCreateUserWithoutSecretFields()
  {
    PFUserDO user = new PFUserDO();
    assertFalse(user.hasSecretFieldValues());
    user.setPassword("test");
    assertTrue(user.hasSecretFieldValues());
    user.setPassword(null);
    user.setStayLoggedInKey("test");
    assertTrue(user.hasSecretFieldValues());
    user.setAuthenticationToken("test");
    user.setStayLoggedInKey(null);
    assertTrue(user.hasSecretFieldValues());
    user.setAuthenticationToken(null);
    user.setPasswordSalt("test");
    assertTrue(user.hasSecretFieldValues());
    user.setPasswordSalt(null);
    assertFalse(user.hasSecretFieldValues());
    user.setPassword("pw");
    user.setPasswordSalt("ps");
    user.setAuthenticationToken("at");
    user.setStayLoggedInKey("st");
    assertEquals("pw", user.getPassword());
    assertEquals("ps", user.getPasswordSalt());
    assertEquals("at", user.getAuthenticationToken());
    assertEquals("st", user.getStayLoggedInKey());
    user = PFUserDO.Companion.createCopyWithoutSecretFields(user);
    assertNull(user.getPassword());
    assertNull(user.getPasswordSalt());
    assertNull(user.getAuthenticationToken());
    assertNull(user.getStayLoggedInKey());
  }

  @Test
   void testToString() {
    PFUserDO user = new PFUserDO();
    user.setUsername("test");
    user.setPassword("123");
    user.setAuthenticationToken("123");
    user.setPasswordSalt("123");
    user.setStayLoggedInKey("123");
    String str = user.toString();
    assertFalse(str.contains("123"), "Secret fields must be ommitted!");
  }

  @Test
   void testCopyValues() {
    PFUserDO user = new PFUserDO();
    user.setUsername("test");
    user.setPassword("123");
    user.setAuthenticationToken("123");
    user.setPasswordSalt("123");
    user.setStayLoggedInKey("123");
    PFUserDO user2 = PFUserDO.createCopyWithoutSecretFields(user);
    assertFalse(user2.hasSecretFieldValues());
    assertEquals("test", user2.getUsername());
    user2 = new PFUserDO();
    user2.copyValuesFrom(user);
    assertTrue(user2.hasSecretFieldValues());
    assertEquals("test", user2.getUsername());
  }

  @Test
  void testDisplayName() {
    PFUserDO user = new PFUserDO();
    user.setUsername("kai");
    assertEquals("kai", user.getUserDisplayName());
    user.setFirstname("Kai");
    assertEquals("Kai (kai)", user.getUserDisplayName());
    user.setLastname("Reinhard");
    assertEquals("Kai Reinhard (kai)", user.getUserDisplayName());
    user.setFirstname(null);
    assertEquals("Reinhard (kai)", user.getUserDisplayName());
  }

  @Test
  void testFullName() {
    PFUserDO user = new PFUserDO();
    assertEquals("", user.getFullname());
    user.setFirstname("Kai");
    assertEquals("Kai", user.getFullname());
    user.setLastname("Reinhard");
    assertEquals("Kai Reinhard", user.getFullname());
    user.setFirstname(null);
    assertEquals("Reinhard", user.getFullname());
  }
}
