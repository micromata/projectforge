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

import static org.junit.jupiter.api.Assertions.*;

public class PFUserDOTest
{
  @Test
  public void testCreateUserWithoutSecretFields()
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
}
