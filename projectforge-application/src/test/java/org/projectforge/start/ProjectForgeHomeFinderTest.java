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
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ProjectForgeHomeFinderTest {
  @Test
  void findBaseDir() throws IOException {
    File tmpDir = Files.createTempDirectory("projectforge-application-basedir-test").toFile();
    File pfDir = new File(tmpDir, "ProjectForge");
    pfDir.mkdir();
    File subDir = new File(pfDir, "subdir");
    subDir.mkdir();


    File dir = ProjectForgeHomeFinder.findBaseDirAndAncestors(subDir);
    assertEquals("ProjectForge", dir.getName());

    dir = ProjectForgeHomeFinder.findBaseDirAndAncestors(new File("."));
    if (dir != null)
      assertFalse(new File(dir, "projectforge-business").exists(), "The source code directory shouldn't be found.");
  }
}
