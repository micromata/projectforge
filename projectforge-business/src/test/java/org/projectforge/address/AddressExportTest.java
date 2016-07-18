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

package org.projectforge.address;

import static org.testng.AssertJUnit.assertEquals;

import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.testng.annotations.Test;

public class AddressExportTest
{
  @Test
  public void testFullname()
  {
    AddressDao addressDao = new AddressDao();
    AddressDO a = new AddressDO();
    assertEquals("", addressDao.getFullName(a));
    a.setFirstName("Kai");
    assertEquals("Kai", addressDao.getFullName(a));
    a.setFirstName(null);
    a.setName("Reinhard");
    assertEquals("Reinhard", addressDao.getFullName(a));
    a.setFirstName("Kai");
    assertEquals("Reinhard Kai", addressDao.getFullName(a));
    a.setTitle("Dipl.-Phys.");
    assertEquals("Reinhard Kai Dipl.-Phys.", addressDao.getFullName(a));

    a.setFirstName(null);
    a.setName(null);
    assertEquals("Dipl.-Phys.", addressDao.getFullName(a));
    a.setFirstName("Kai");
    assertEquals("Kai Dipl.-Phys.", addressDao.getFullName(a));
    a.setFirstName(null);
    a.setName("Reinhard");
    assertEquals("Reinhard Dipl.-Phys.", addressDao.getFullName(a));
  }
}
