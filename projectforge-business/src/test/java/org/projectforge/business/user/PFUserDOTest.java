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

package org.projectforge.business.user;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.business.test.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PFUserDOTest extends AbstractTestBase {
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
