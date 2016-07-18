package org.projectforge.framework.persistence.database;

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlDumpService;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface PfJpaXmlDumpService extends JpaXmlDumpService
{
  int createTestDatabase();

}
