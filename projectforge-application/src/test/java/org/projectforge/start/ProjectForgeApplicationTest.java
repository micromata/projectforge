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

package org.projectforge.start;


import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProjectForgeApplicationTest {
  static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeApplicationTest.class);

  @Test
  void addDefaultAdditionalLocation() {
    String loc = ProjectForgeApplication.getAddtionalLocationArg(null);
    if (ProjectForgeApplication.addDefaultAdditionalLocation(null, null) == null) {
      log.warn("Found application{-default}.properties in current working directory (you should move it to ~/ProjectForge/projectforge.properties). Can't process with this text (OK).");
      return;
    }
    checkArray(new String[]{loc}, null);
    checkArray(new String[]{loc}, new String[]{});
    checkArray(new String[]{"spring.datasource.driver-class-name=org.postgresql.Driver", loc}, new String[]{"spring.datasource.driver-class-name=org.postgresql.Driver"});
    checkArray(new String[]{"--spring.config.additional-location=file:/opt/projectforge/test.properties"}, new String[]{"--spring.config.additional-location=file:/opt/projectforge/test.properties"});
    checkArray(new String[]{"hurzel", "--spring.config.additional-location=file:/opt/projectforge/test.properties"}, new String[]{"hurzel", "--spring.config.additional-location=file:/opt/projectforge/test.properties"});
  }

  @Test
  void findBaseDir() throws IOException {
    ProjectForgeApplication.findBaseDir(new File("/"));
    File tmpDir = Files.createTempDirectory("projectforge-application-basedir-test").toFile();
    File pfDir = new File(tmpDir, "ProjectForge");
    pfDir.mkdir();
    File subDir = new File (pfDir, "subdir");
    subDir.mkdir();
    File dir = ProjectForgeApplication.findBaseDir(subDir);
    assertEquals("ProjectForge", dir.getName());
  }

  private void checkArray(String[] expected, String[] array) {
    String[] args = ProjectForgeApplication.addDefaultAdditionalLocation(null, array);
    assertEquals(expected.length, args.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], args[i]);
    }
  }
}
