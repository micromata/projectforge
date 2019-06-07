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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LdapSambaAccountConfigTest
{
  @Test
  public void getSambaSID()
  {
    LdapSambaAccountsConfig config = new LdapSambaAccountsConfig().setSambaSIDPrefix("123-456");
    assertEquals("123-456-789", config.getSambaSID(789));
    assertEquals("123-456-???", config.getSambaSID(null));
    config = new LdapSambaAccountsConfig().setSambaSIDPrefix(null);
    assertEquals("S-000-000-000-789", config.getSambaSID(789));
    assertEquals("S-000-000-000-???", config.getSambaSID(null));
  }

  @Test
  public void getSambaSIDNumber()
  {
    final LdapSambaAccountsConfig config = new LdapSambaAccountsConfig().setSambaSIDPrefix("123-456");
    assertEquals(789, config.getSambaSIDNumber("123-456-789").intValue());
    assertEquals(789, config.getSambaSIDNumber("-789").intValue());
    assertEquals(1, config.getSambaSIDNumber("-1").intValue());
    assertNull(config.getSambaSIDNumber("123456789"));
    assertNull(config.getSambaSIDNumber(""));
    assertNull(config.getSambaSIDNumber("-"));
  }
}
