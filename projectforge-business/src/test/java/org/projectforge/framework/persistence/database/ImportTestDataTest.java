package org.projectforge.framework.persistence.database;

import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
public class ImportTestDataTest extends AbstractTestBase
{
  @Autowired
  private InitDatabaseDao initDatabaseDao;

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
    jpaXmlDumpService.createTestDatabase();
  }

}
