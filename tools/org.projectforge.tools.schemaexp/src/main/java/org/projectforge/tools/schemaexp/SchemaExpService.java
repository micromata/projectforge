package org.projectforge.tools.schemaexp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.persistence.database.XmlDump;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.xstream.XStreamSavingConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlDumpService;
import de.micromata.genome.db.jpa.xmldump.api.JpaXmlDumpService.RestoreMode;
import de.micromata.genome.util.runtime.RuntimeIOException;

@Service
public class SchemaExpService
{
  private static final Logger LOG = Logger.getLogger(SchemaExpService.class);
  @Autowired
  TransactionTemplate txTemplate;
  @Autowired
  private DataSource dataSource;

  @Autowired
  PfEmgrFactory emfac;

  @Autowired
  private XmlDump xmlDump;

  @Autowired
  private ConfigurationDao configurationDao;

  @Autowired
  private JpaXmlDumpService jpaXmlDumpService;
  private boolean useJpaXmlDumpService = true;

  public void clearDb()
  {
    emfac.getJpaSchemaService().clearDatabase();
  }

  public void doImport(boolean clearDb, String clientFileName, RestoreMode modus)
  {
    if (clearDb == true) {
      clearDb();
    }
    File infile = new File(clientFileName);
    if (infile.exists() == false) {
      throw new RuntimeIOException("File does not exists: " + infile.getAbsolutePath());
    }
    InputStream is = null;
    try {

      if (clientFileName.endsWith(".xml.gz") == true) {
        is = new GZIPInputStream(new FileInputStream(infile));
      } else if (clientFileName.endsWith(".xml") == true) {
        is = new FileInputStream(infile);
      } else {
        throw new RuntimeIOException("Expect file with .xml or .xml.gz");
      }
      if (useJpaXmlDumpService == true) {
        jpaXmlDumpService.restoreDb(emfac, is, modus);
      } else {
        Reader reader = new InputStreamReader(is);
        final XStreamSavingConverter converter = xmlDump.restoreDatabase(reader);
        final int counter = xmlDump.verifyDump(converter);
        configurationDao.checkAndUpdateDatabaseEntries();
      }
    } catch (IOException ex) {
      throw new RuntimeIOException(ex);
    } finally {
      IOUtils.closeQuietly(is);
    }

  }

  public void doExport(String fileName)
  {
    File file = new File(fileName);
    jpaXmlDumpService.dumpToXml(emfac, file);

  }
}
