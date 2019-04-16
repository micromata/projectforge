package org.projectforge.framework.persistence.database;

import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;
import org.projectforge.test.InitTestDB;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class ImportTestDataTest extends AbstractTestBase {
  @Autowired
  private DatabaseService initDatabaseDao;

  @Autowired
  private InitTestDB initTestDB;

  @Autowired
  private PfJpaXmlDumpService jpaXmlDumpService;

  @Override
  protected void beforeAll() {
    recreateDataBase();
  }

  @Override
  protected void afterAll() {
    clearDatabase();
  }

  @Override
  protected void initDb() {
    // no intial entities.
    init(false);
  }

  @Test
  public void testImport() {
    clearDatabase();
    initDatabaseDao.insertDefaultTenant();
    PFUserDO admin = initTestDB.addUser(AbstractTestBase.ADMIN);
    initDatabaseDao.insertGlobalAddressbook(admin);
    jpaXmlDumpService.createTestDatabase();
  }

}
