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

package org.projectforge.framework.persistence.database;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.business.test.AbstractTestBase;
import org.projectforge.business.test.InitTestDB;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class ImportTestDataTest extends AbstractTestBase {
  @Autowired
  private DatabaseService databaseService;

  @Autowired
  private InitTestDB initTestDB;

  //@Autowired
  //private PfJpaXmlDumpService jpaXmlDumpService;

  @Override
  protected void beforeAll() {
    clearDatabase();
  }

  @Override
  protected void afterAll() {
    clearDatabase();
  }

  @Override
  protected void initDb() {
    // no initial entities.
    init(false);
  }

  @Test
  public void testImport() {
    clearDatabase();
    PFUserDO admin = initTestDB.addUser(AbstractTestBase.ADMIN);
    databaseService.insertGlobalAddressbook(admin);
    //jpaXmlDumpService.createTestDatabase();
  }

}
