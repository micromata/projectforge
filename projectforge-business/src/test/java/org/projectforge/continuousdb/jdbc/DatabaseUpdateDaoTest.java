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

package org.projectforge.continuousdb.jdbc;

import static org.testng.AssertJUnit.assertEquals;

import org.projectforge.continuousdb.DatabaseUpdateService;
import org.projectforge.continuousdb.UpdaterConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DatabaseUpdateDaoTest
{

  @Test
  public void createUniqueConstraintName()
  {
    final DatabaseUpdateService databaseUpdateDao = new DatabaseUpdateService();
    databaseUpdateDao.setConfiguration(new UpdaterConfiguration());
    final String[] existingConstraintNames = { "t_mytable_uq_tenant_i1", "t_mytable_uq_tenant_i2",
        "t_mytable_uq_username",
        "t_my_very_long__uq_tenant_i1", "t_my_very_long__uq_tenant_i2", "t_my_very_long__uq_username" };
    assertEquals("t_mytable_uq_tenant_i3",
        databaseUpdateDao.createUniqueConstraintName("t_MYTABLE", new String[] { "Tenant_id", "username" },
            existingConstraintNames));
    assertEquals("t_mytable_uq_name1",
        databaseUpdateDao.createUniqueConstraintName("t_mytable", new String[] { "name", "username" },
            existingConstraintNames));
    assertEquals("t_my_very_long__uq_tenant_i3",
        databaseUpdateDao.createUniqueConstraintName("t_my_very_LONG_table", new String[] { "tenant_id", "username" },
            existingConstraintNames));
    assertEquals("t_my_very_long__uq_name1",
        databaseUpdateDao.createUniqueConstraintName("t_my_very_long_table", new String[] { "Name", "username" },
            existingConstraintNames));

    final String[] paranoia = new String[1000];
    for (int i = 0; i < 1000; i++) {
      paranoia[i] = "t_mytable_uq_tenant_i" + i;
    }
    try {
      databaseUpdateDao.createUniqueConstraintName("t_mytable", new String[] { "tenant_id", "username" }, paranoia);
      Assert.fail("UnsupportedOperation excepted!");
    } catch (final UnsupportedOperationException ex) {
      // Expected.
    }
  }
}
