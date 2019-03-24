package org.projectforge.framework.persistence.database;

import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestNGBase;
import org.projectforge.test.InitTestDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class ImportTestDataTest extends AbstractTestNGBase
{
  @Autowired
  private DatabaseService initDatabaseDao;

  @Autowired
  private InitTestDB initTestDB;

  @Autowired
  private PfJpaXmlDumpService jpaXmlDumpService;

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
    initDatabaseDao.insertDefaultTenant();
    PFUserDO admin = initTestDB.addUser(AbstractTestNGBase.ADMIN);
    initDatabaseDao.insertGlobalAddressbook(admin);
    jpaXmlDumpService.createTestDatabase();
  }

}
