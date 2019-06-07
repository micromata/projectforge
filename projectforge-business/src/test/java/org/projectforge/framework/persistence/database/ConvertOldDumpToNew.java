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

package org.projectforge.framework.persistence.database;

import java.io.File;

import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlDumpService;

public class ConvertOldDumpToNew extends AbstractTestBase
{
  @Autowired
  private JpaXmlDumpService jpaXmlDumpService;
  @Autowired
  private XmlDump xmlDump;
  @Autowired
  private PfEmgrFactory emfac;

  @Override
  protected void initDb()
  {
    // no intial entities.
    init(false);
  }

  @Test
  public void testImport()
  {
    clearDatabase();
    xmlDump.restoreDatabaseFromClasspathResource("/data/init-test-data.xml", "UTF-8");
    jpaXmlDumpService.dumpToXml(emfac, new File("target/init-test-data-new.xml"));
    //    InputStream is = InitDatabaseDao.openCpInputStream(InitDatabaseDao.TEST_DATA_BASE_DUMP_FILE);
    //    jpaXmlDumpService.restoreDb(emfac, is, RestoreMode.InsertAll);

  }

  @Override
  protected void clearDatabase()
  {

  }

}
