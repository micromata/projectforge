package org.projectforge.framework.persistence.database;

import java.io.File;

import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.test.AbstractTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

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
