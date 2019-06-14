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

package org.projectforge.common.development;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectforge.common.BeanHelper;

import java.io.File;
import java.io.IOException;
import java.time.Year;

/**
 * Modifies the file header of each source file containing the license.
 */
public class SourceFileHeadersTest {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BeanHelper.class);

  private static final int YEAR = Year.now().getValue();

  private static String OSS_HEADER;

  private static String CLOSED_HEADER;

  @Test
  void validateJavaKotlinCopyRightFileHeaders() throws IOException {
    File baseDir = new File(System.getProperty("user.dir")).getParentFile();
    new SourceFileHeadersMain(baseDir).validateAndFixAllProjectForgeHeaders(false);
  }

  @Test
  void checkMainJavaFile() throws IOException {
    File baseDir = new File(System.getProperty("user.dir")).getParentFile();
    File mainJavaFile = new SourceFileHeadersMain(baseDir).getMainJavaFile();
    Assertions.assertTrue(mainJavaFile.canRead(), "Can't read java main file: " + mainJavaFile.getAbsolutePath());
  }
}
