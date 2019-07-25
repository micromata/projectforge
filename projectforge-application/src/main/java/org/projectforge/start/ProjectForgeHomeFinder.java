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

import org.projectforge.common.LoggerSupport;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for finding ProjectForge's home directory:<br/>
 * <ol>
 * <li>Create ProjectForge as a top level directory of your home directory: '$HOME/ProjectForge', or</li>
 * <li>create a directory named 'ProjectForge' and put the jar file somewhere in it or in the same directory. ProjectForge detects the folder 'ProjectForge' relative to the executed jar, or</li>
 * <li>create a directory and define it as command line parameter: java -D" + COMMAND_LINE_VAR_HOME_DIR + "=yourdirectory -jar ..., or</li>
 * <li>create a directory and define it as system environment variable $PROJECTFORGE_HOME.</li>
 * </ol>
 */
public class ProjectForgeHomeFinder {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeHomeFinder.class);

  private static final String ENV_PROJECTFORGE_HOME = "PROJECTFORGE_HOME";

  private static final String COMMAND_LINE_VAR_HOME_DIR = "home.dir";

  private static final String[] DIR_NAMES = {"ProjectForge", "Projectforge", "projectforge"};

  static void giveUp() {
    new LoggerSupport(log, LoggerSupport.Alignment.LEFT)
            .log("Your options (please refer: https://github.com/micromata/projectforge):")
            .log("  1. Create ProjectForge as a top level directory of your home directory:")
            .log("     '$HOME/ProjectForge', or")
            .log("  2. create a directory named 'ProjectForge' and put the jar file somewhere in")
            .log("     it or in the same directory. ProjectForge detects the folder 'ProjectForge'")
            .log("     relative to the executed jar, or")
            .log("  3. create a directory and define it as command line parameter:")
            .log("     'java -D" + COMMAND_LINE_VAR_HOME_DIR + "=yourdirectory -jar ...', or")
            .log("  4. create a directory and define it as system environment variable")
            .log("     '" + ENV_PROJECTFORGE_HOME + "'.")
            .log("Hope to see You again ;-)")
            .logEnd();
    System.exit(1);
  }

  /**
   * Searches for the ProjectForge dir in the given baseDir and all its parent directories. If nothing found, the user's
   * home directory is searched.
   *
   * @param baseDir
   * @return
   */
  static File findBaseDir(File baseDir) {
    // Search the given baseDir and all parent dirs:
    File dir = findBaseDirAndAncestors(baseDir);
    if (dir != null) {
      return dir;
    }

    File jarFileDir = getExecutableDir(false);
    dir = findBaseDirAndAncestors(jarFileDir);
    if (dir != null) {
      log.info("Using location relative to running jar: " + dir.getAbsolutePath());
      return dir;
    }

    // Search the user's home dir:
    String userHome = System.getProperty("user.home");
    return findBaseDirOnly(new File(userHome));
  }

  public static File[] getSuggestedDirectories() {
    List<File> files = new ArrayList<>();
    checkAndAdd(files, System.getProperty("user.home"));
    checkAndAdd(files, getExecutableDir(true));
    checkAndAdd(files, new File("ProjectForge"));
    return files.toArray(new File[files.size()]);
  }

  private static void checkAndAdd(List<File> files, String path) {
    checkAndAdd(files, new File(path, "ProjectForge"));
  }

  private static void checkAndAdd(List<File> files, File dir) {
    if (isProjectForgeSourceCodeRepository(dir))
      return;
    files.add(dir.getAbsoluteFile());
  }

  private static File getExecutableDir(boolean includingSourceCodeRepository) {
    try {
      URL locationUrl = ProjectForgeHomeFinder.class.getProtectionDomain().getCodeSource().getLocation();
      String location = locationUrl.toExternalForm();
      if (location.startsWith("jar:")) {
        location = location.substring(4);
      } else if (!includingSourceCodeRepository) {
        // Development source code, don't use the ProjectForge source code repository as working directory directly:
        return null;
      }
      if (location.indexOf('!') > 0) {
        location = location.substring(0, location.indexOf('!'));
      }
      return new File(new URI(location));
    } catch (URISyntaxException ex) {
      log.error("Internal error while trying to get the location of ProjectForge's running code: " + ex.getMessage(), ex);
      return null;
    }
  }

  private static File findBaseDirAndAncestors(File baseDir) {
    // Need absolute directory to check parent directories.
    File currentDir = baseDir.isAbsolute() ? baseDir : new File(baseDir.getAbsolutePath());
    int recursiveCounter = 100; // Soft links may result in endless loops.
    do {
      File dir = findBaseDirOnly(currentDir);
      if (dir != null) {
        return dir;
      }
      currentDir = currentDir.getParentFile();
    } while (currentDir != null && --recursiveCounter > 0);
    return null;
  }

  private static File findBaseDirOnly(File baseDir) {
    if (!baseDir.exists() || !baseDir.isDirectory()) {
      return null;
    }
    for (String path : DIR_NAMES) {
      File dir = new File(baseDir, path);
      if (checkDirectory(dir, false))
        return dir;
    }
    return null;
  }

  static boolean checkDirectory(File baseDir, boolean logWarning) {
    if (!baseDir.exists()) {
      if (logWarning)
        log.warn("Configured base dir '" + baseDir.getAbsolutePath() + "' doesn't exist. Ignoring it.");
      return false;
    }
    if (!baseDir.isDirectory()) {
      if (logWarning)
        log.warn("Configured base dir '" + baseDir.getAbsolutePath() + "' is not a directory. Ignoring it.");
      else
        log.warn("'" + baseDir.getAbsolutePath() + "' found, but isn't a directory, ignoring...");
      return false;
    }
    // Check for ProjectForge as source code repository:
    if (isProjectForgeSourceCodeRepository(baseDir)) {
      if (logWarning) {
        log.warn("Configured base dir '" + baseDir.getAbsolutePath() + "' seems to be the source code repository and shouldn't be used. Ignoring it.");
      }
      return false;
    }
    return true;
  }

  public static boolean isProjectForgeSourceCodeRepository(File dir) {
    File current = dir;
    //int recursiveCounter = 100; // Soft links may result in endless loops.
    //do {
    if (current.exists()
            && new File(current, "projectforge-application").exists()
            && new File(current, "projectforge-business").exists()
            && new File(current, "projectforge-common").exists()) {
      return true;
    }
    //   current = current.getParentFile();
    // } while (current != null && --recursiveCounter > 0);
    return false;
  }
}
