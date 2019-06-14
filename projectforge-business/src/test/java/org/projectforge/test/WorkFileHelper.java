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

package org.projectforge.test;

import java.io.File;

public class WorkFileHelper
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkFileHelper.class);

  private static final String WORK_DIR = "./target/work";

  /**
   * Get the file from the working directory. If the working directory doesn't exist then it'll be created.
   * 
   * @param filename
   * @return
   */
  public static File getWorkFile(final String filename)
  {
    final File workDir = new File(WORK_DIR);
    if (workDir.exists() == false) {
      log.info("Create working directory: " + workDir.getAbsolutePath());
      workDir.mkdir();
    }
    return new File(workDir, filename);
  }

}
