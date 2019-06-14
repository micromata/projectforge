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
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Modifies the file header of each source file containing the license.
 */
public class SourceFileHeadersMain {

  private static final int YEAR = Year.now().getValue();

  private static String OSS_HEADER;

  private static String CLOSED_HEADER;

  private int modifiedFilesCounter = 0;

  private File mainJavaFile;

  private File baseDir;

  private List<File> fixedFiles = new ArrayList<>();

  public static void main(final String[] args) throws IOException {
    File baseDir = new File(System.getProperty("user.dir"));
    new SourceFileHeadersMain(baseDir).validateAndFixAllProjectForgeHeaders(true);
  }

  SourceFileHeadersMain(File baseDir) {
    this.baseDir = baseDir;
    final Collection<File> files = FileUtils.listFiles(baseDir, new String[]{"java"}, true);
    String filename = this.getClass().getName().replace('.', File.separatorChar) + ".java";
    for (final File file : files) {
      if (file.getAbsolutePath().contains("src/test/java")
              && file.getAbsolutePath().endsWith(filename)) {
        mainJavaFile = file;
      }
    }
  }

  File getMainJavaFile() {
    return mainJavaFile;
  }

  void validateAndFixAllProjectForgeHeaders(final boolean autoFixFiles) throws IOException {
    validateAndFixHeaders(new File(baseDir, "projectforge-application").getAbsolutePath(), true, autoFixFiles);
    validateAndFixHeaders(new File(baseDir, "projectforge-business").getAbsolutePath(), true, autoFixFiles);
    validateAndFixHeaders(new File(baseDir, "projectforge-common").getAbsolutePath(), true, autoFixFiles);
    validateAndFixHeaders(new File(baseDir, "projectforge-model").getAbsolutePath(), true, autoFixFiles);
    validateAndFixHeaders(new File(baseDir, "projectforge-rest").getAbsolutePath(), true, autoFixFiles);
    validateAndFixHeaders(new File(baseDir, "projectforge-webapp").getAbsolutePath(), true, autoFixFiles);
    final File[] files = new File(baseDir, "plugins").listFiles();
    for (File file : files) {
      if (!file.isDirectory() || !file.getName().startsWith("org.projectforge.plugins")) continue;
      validateAndFixHeaders(file.getAbsolutePath(), true, autoFixFiles);
    }
    if (modifiedFilesCounter > 0) {
      System.out.println("Following source code file headers were fixed (" + modifiedFilesCounter + " files).");
      for (File file : fixedFiles) {
        System.out.println(file.getAbsolutePath());
      }
      System.out.println("Total: " + modifiedFilesCounter + " files.");
    }
  }

  private void validateAndFixHeaders(final String path, final boolean openSource, final boolean autoFixFiles) throws IOException {
    if (autoFixFiles) {
      if (openSource) {
        System.out.println("OSS: Validating and fixing all source code file headers in: " + path);
      } else {
        System.out.println("Closed: Validating and fixing all source code file headers in: " + path);
      }
    }
    final Collection<File> files = FileUtils.listFiles(new File(path, "src"), new String[]{"java", "kt"}, true);
    for (final File file : files) {
      if (file.getAbsolutePath().contains("org/projectforge/lucene/PF")
              || file.getAbsolutePath().contains("arlut/csd/crypto")
              || file.getAbsolutePath().contains("at/jta")
              || file.getAbsolutePath().contains("edu/stanford")
              || file.getAbsolutePath().contains("java/net")
              || file.getAbsolutePath().contains("name/fraser/neil/plaintext")
              || file.getAbsolutePath().contains("org/lesscss")
              || file.getAbsolutePath().contains("org/parosproxy")
              || file.getAbsolutePath().contains("org/projectforge/lucene/Classic") // ClassicAnalyzer, ClassicFilter, ...
              || file.getAbsolutePath().contains("org/projectforge/lucene/Standard")  // StandardAnalyzer, ...
              || file.getAbsolutePath().contains("org/projectforge/lucene/UAX29")
              || file.getAbsolutePath().contains("org/zaproxy")) {
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
      if (!autoFixFiles) {
        Assertions.fail("Source code file '" + file.getName() + "' without valid copy right header. As a maintainer you should fix it by simply calling Java main: "
                + mainJavaFile.getAbsolutePath());
      }
      System.out.println("****** Source code file without valid copy right header (will be fixed right now automatically): " + file.getAbsolutePath());
      fixedFiles.add(file);
      FileReader reader = null;
      LineNumberReader in = null;
      final StringBuilder sb = new StringBuilder();
      try {
        reader = new FileReader(file);
        in = new LineNumberReader(reader);
        String line = "";
        boolean header = true;
        if (openSource) {
          sb.append(OSS_HEADER);
        } else {
          sb.append(CLOSED_HEADER);
        }
        while ((line = in.readLine()) != null) {
          if (header) {
            if (line.trim().startsWith("//") || line.trim().length() == 0) {
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
      modifiedFilesCounter++;
    }
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
