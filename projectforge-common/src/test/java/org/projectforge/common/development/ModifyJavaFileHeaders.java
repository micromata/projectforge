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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.projectforge.common.BeanHelper;

import java.io.*;
import java.time.Year;
import java.util.Collection;

/**
 * Modifies the file header of each source file containing the license.
 */
public class ModifyJavaFileHeaders {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BeanHelper.class);

  private static final int YEAR = Year.now().getValue();

  private static String OSS_HEADER;

  private static String CLOSED_HEADER;

  public static void _main(final String[] args) throws IOException {
    if (args != null && args.length == 1 && "PF-AUTO".equals(args[0])) {
      new ModifyJavaFileHeaders().createAllProjectForgeHeaders();
      return;
    }
    if (args == null || args.length != 2) {
      System.err.println("Type (OSS, NONE or CLOSED) and directory or PF-AUTO expected as arguments.");
      System.exit(1);
    }
    final String type = args[0];
    if ("NONE".equals(type) == true) {
      // Nothing to do.
      System.exit(0);
    }
    if ("OSS".equals(type) == false && "CLOSED".equals(type) == false) {
      System.err.println("Argument 0 (type) must be OSS, NONE or CLOSED.");
      System.exit(1);
    }
    final String dir = args[1] + File.separatorChar + "src";
    new ModifyJavaFileHeaders().doitReally(dir, "OSS".equals(type));
  }

  @Test
  void checkAndFixAllJavaKotlinCopyRightFileHeaders() throws IOException {
    new ModifyJavaFileHeaders().createAllProjectForgeHeaders();
  }

  private void createAllProjectForgeHeaders() throws IOException {
    File baseDir = new File(System.getProperty("user.dir")).getParentFile();
    log.info("Using working directory: " + baseDir.getAbsolutePath());
    doitReally(new File(baseDir, "projectforge-application").getAbsolutePath(), true);
    doitReally(new File(baseDir, "projectforge-business").getAbsolutePath(), true);
    doitReally(new File(baseDir, "projectforge-common").getAbsolutePath(), true);
    doitReally(new File(baseDir, "projectforge-model").getAbsolutePath(), true);
    doitReally(new File(baseDir, "projectforge-rest").getAbsolutePath(), true);
    doitReally(new File(baseDir, "projectforge-webapp").getAbsolutePath(), true);
    final File[] files = new File(baseDir, "plugins").listFiles();
    for (File file : files) {
      if (!file.isDirectory() || !file.getName().startsWith("org.projectforge.plugins")) continue;
      doitReally(file.getAbsolutePath(), true);
    }

  }

  private ModifyJavaFileHeaders doitReally(final String path, final boolean openSource) throws IOException {
    if (openSource == true) {
      log.info("OSS: Modify all Java file headers: " + path + " *******");
    } else {
      log.info("Closed: Modify all Java file headers: " + path);
    }
    final Collection<File> files = FileUtils.listFiles(new File(path), new String[]{"java", "kt"}, true);
    int counter = 0;
    for (final File file : files) {
      if (file.getAbsolutePath().contains("org/projectforge/lucene/PF") == true
              || file.getAbsolutePath().contains("arlut/csd/crypto") == true
              || file.getAbsolutePath().contains("at/jta") == true
              || file.getAbsolutePath().contains("edu/stanford") == true
              || file.getAbsolutePath().contains("java/net") == true
              || file.getAbsolutePath().contains("name/fraser/neil/plaintext") == true
              || file.getAbsolutePath().contains("org/lesscss") == true
              || file.getAbsolutePath().contains("org/parosproxy") == true
              || file.getAbsolutePath().contains("org/projectforge/lucene/Classic") == true // ClassicAnalyzer, ClassicFilter, ...
              || file.getAbsolutePath().contains("org/projectforge/lucene/Standard") == true // StandardAnalyzer, ...
              || file.getAbsolutePath().contains("org/projectforge/lucene/UAX29") == true
              || file.getAbsolutePath().contains("org/zaproxy") == true) {
        continue;
      }
      String content = FileUtils.readFileToString(file, "UTF-8");
      if (openSource && content.startsWith(OSS_HEADER)) {
        // Header up-to-date
        continue;
      }
      if (!openSource && content.startsWith(CLOSED_HEADER)) {
        // Header up-to-date
        continue;
      }
      log.warn("Source code file without valid copy right header (will be fixed now):  " + file.getAbsolutePath());
      FileReader reader = null;
      LineNumberReader in = null;
      final StringBuilder sb = new StringBuilder();
      try {
        reader = new FileReader(file);
        log.debug("Processing '" + file.getAbsolutePath() + "'.");
        in = new LineNumberReader(reader);
        String line = "";
        boolean header = true;
        if (openSource == true) {
          sb.append(OSS_HEADER);
        } else {
          sb.append(CLOSED_HEADER);
        }
        while ((line = in.readLine()) != null) {
          if (header == true) {
            if (line.trim().startsWith("//") == true || line.trim().length() == 0) {
              continue;
            } else {
              header = false;
            }
          }
          sb.append(line).append("\n");
        }
      } finally {
        if (reader != null) {
          reader.close();
        }
        if (in != null) {
          in.close();
        }
      }
      FileWriter out = null;
      try {
        out = new FileWriter(file);
        out.write(sb.toString());
      } finally {
        if (out != null) {
          out.close();
        }
      }
      counter++;
    }
    log.info("All Java files were modified (" + counter + " files).");
    return this;
  }

  static {
    StringBuilder sb = new StringBuilder();
    sb.append("/////////////////////////////////////////////////////////////////////////////\n").append("//\n")
            .append("// Project ProjectForge Community Edition\n")
            .append("//         www.projectforge.org\n")
            .append("//\n")
            .append("// Copyright (C) 2001-").append(YEAR).append(" Micromata GmbH, Germany (www.micromata.com)\n")
            .append("//\n")
            .append("// ProjectForge is dual-licensed.\n")
            .append("//\n")
            .append("// This community edition is free software; you can redistribute it and/or\n")
            .append("// modify it under the terms of the GNU General Public License as published\n")
            .append("// by the Free Software Foundation; version 3 of the License.\n")
            .append("//\n")
            .append("// This community edition is distributed in the hope that it will be useful,\n")
            .append("// but WITHOUT ANY WARRANTY; without even the implied warranty of\n")
            .append("// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General\n")
            .append("// Public License for more details.\n")
            .append("//\n")
            .append("// You should have received a copy of the GNU General Public License along\n")
            .append("// with this program; if not, see http://www.gnu.org/licenses/.\n")
            .append("//\n")
            .append("/////////////////////////////////////////////////////////////////////////////\n\n");
    OSS_HEADER = sb.toString();
    sb = new StringBuilder();
    sb.append("/////////////////////////////////////////////////////////////////////////////\n").append("//\n")
            .append("// Project ProjectForge Enterprise Edition\n")
            .append("//         www.projectforge.org\n")
            .append("//\n")
            .append("// Copyright (C) 2001-\").append(YEAR).append(\" Micromata GmbH, Germany (www.micromata.com)\n")
            .append("//\n")
            .append("// All rights reserved.\n")
            .append("//\n")
            .append("// You're not allowed to distribute or use this code without the permit\n")
            .append("// of Micromata GmbH (www.micromata.com).\n")
            .append("//\n")
            .append("/////////////////////////////////////////////////////////////////////////////\n\n");
    CLOSED_HEADER = sb.toString();
  }
}
