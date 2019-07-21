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

package org.projectforge.flyway.dbmigration;

import org.apache.commons.io.FileUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.projectforge.business.configuration.ConfigurationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Since ProjectForge version 7 a new Hibernate search version is used. This version is based on Lucene 54 and the
 * old index files (Lucene53) have to be removed.
 */
public class V7_0_0_2__DropLuceneIndex extends BaseJavaMigration {
  private static Logger log = LoggerFactory.getLogger(V7_0_0_2__DropLuceneIndex.class);

  @Override
  public void migrate(Context context) throws Exception {
    File file = new File(ConfigurationServiceImpl.getStaticApplicationHomeDir(), "hibernateSearch");
    log.info("Deleting database index (from former Lucene index). The index has to be rebuilt. Removing lucene index directory: " + file.getAbsolutePath());
    FileUtils.deleteDirectory(file);
  }
}
