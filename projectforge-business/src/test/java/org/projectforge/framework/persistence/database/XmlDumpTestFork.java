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

package org.projectforge.framework.persistence.database;

import org.projectforge.business.user.UserCache;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

public class XmlDumpTestFork extends AbstractTestBase
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(XmlDumpTestFork.class);

  @Autowired
  private InitDatabaseDao initDatabaseDao;

  @Autowired
  private XmlDump xmlDump;

  @Autowired
  UserCache userCache;

  @Override
  protected void initDb()
  {
    // no intial entities.
    init(false);
  }

  @Test(enabled = false)
  public void verifyDump()
  {
    //    TenantRegistryMap.getInstance().setAllUserGroupCachesAsExpired();
    //    userCache.setExpired();
    //    assertTrue(initDatabaseDao.isEmpty());
    //        final XStreamSavingConverter converter = xmlDump
    //            .restoreDatabaseFromClasspathResource(InitDatabaseDao.TEST_DATA_BASE_DUMP_FILE, "utf-8");
    //    final int counter = xmlDump.verifyDump(converter);
    //    assertTrue("Import was not successful.", counter > 0);
    //    assertTrue("Minimum expected number of tested object to low: " + counter + " < 50.", counter >= 50);
    //    final PFUserDO user = userDao.internalLoadAll().get(0);
    //    user.setUsername("changed");
    //    userDao.internalUpdate(user);
    //    log.info("****** { The following import error from XmlDump are OK.");
    //    assertEquals("Error should be detected.", -counter, xmlDump.verifyDump(converter));
    //    log.info("****** } The previous import error from XmlDump are OK.");
  }
}

