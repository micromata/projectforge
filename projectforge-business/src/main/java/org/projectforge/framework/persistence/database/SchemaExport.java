/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class SchemaExport
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SchemaExport.class);

  /**
   * Generates the database schema for the current configured database.
   *
   * @param filename Write the schema to the given file. No file output, if null.
   */
  public void exportSchema(String filename)
  {
    Map<String, String> props = new HashMap<>();

    props.put("jakarta.persistence.schema-generation.scripts.action", "create");
    props.put("jakarta.persistence.schema-generation.scripts.create-target", filename);
    Persistence.generateSchema("org.projectforge.webapp", props);

  }
}
