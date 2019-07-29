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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.projectforge.ProjectForgeApp;
import org.projectforge.common.CanonicalFileUtils;
import org.projectforge.common.EmphasizedLogSupport;
import org.projectforge.setup.ProjectForgeInitializer;
import org.projectforge.setup.SetupData;
import org.projectforge.setup.wizard.lanterna.LantSetupWizard;
import org.projectforge.setup.wizard.swing.SwingSetupWizard;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for finding ProjectForge's home directory:<br/>
 * <ol>
 * <li>Create ProjectForge as a top level directory of your home directory: '$HOME/ProjectForge', or</li>
 * <li>create a directory and put the jar file somewhere inside this directory. ProjectForge detects the folder relative to the executed jar, or</li>
 * <li>create a directory and define it as command line parameter: java -D" + COMMAND_LINE_VAR_HOME_DIR + "=yourdirectory -jar ..., or</li>
 * <li>create a directory and define it as system environment variable $PROJECTFORGE_HOME.</li>
 * </ol>
 */
public class ProjectForgeHomeFinder {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeHomeFinder.class);

  public static final String ENV_PROJECTFORGE_HOME = "PROJECTFORGE_HOME";

  public static final String COMMAND_LINE_VAR_HOME_DIR = "home.dir";

  private static final String[] DIR_NAMES = {"ProjectForge", "Projectforge", "projectforge"};

  private WizardMode userAcceptsGraphicalTerminal = null;

  private enum WizardMode {DESKTOP, CONSOLE, NONE}

  /**
   * @return %PROJECTFORGE_HOME for Windows, otherwise $PPROJECTFORGE_HOME
   */
  public static String getHomeEnvironmentVariableDefinition() {
    return (SystemUtils.IS_OS_WINDOWS ? "%" : "$") + ENV_PROJECTFORGE_HOME;
  }

  /**
   * Tries to find ProjectForge's home dir. If not found or isn't initialized, a setup wizard is started.
   *
   * @return The home dir. If not found, a System.exit() is done and user information are shown on how to proceed.
   */
  File findAndEnsureAppHomeDir() {
    // Try directory defined through command line: -Dhome.dir:
    File appHomeDir = proceedForced(System.getProperty(COMMAND_LINE_VAR_HOME_DIR),
            "ProjectForge's home dir is defined as command line param, but isn't yet initialized: -D"
                    + COMMAND_LINE_VAR_HOME_DIR + "=$APP_HOME_DIR");
    if (appHomeDir != null)
      return appHomeDir;

    // Try directory defined through command line: -Dprojectforge.base.dir:
    appHomeDir = proceedForced(System.getProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR),
            "ProjectForge's home dir is defined as command line param, but isn't yet initialized: -D"
                    + ProjectForgeApp.CONFIG_PARAM_BASE_DIR + "=$APP_HOME_DIR");
    if (appHomeDir != null)
      return appHomeDir;

    // Try directory defined through environment variable:
    appHomeDir = proceedForced(System.getenv(ENV_PROJECTFORGE_HOME),
            "ProjectForge's home dir is defined as system environment variable $" + ENV_PROJECTFORGE_HOME + ": $APP_HOME_DIR");
    if (appHomeDir != null)
      return appHomeDir;

    // Try directory where the executable jar resides:
    appHomeDir = searchAndProceed(new File(System.getProperty("user.home")),
            "ProjectForge's home directory found in the user's home dir: $APP_HOME_DIR",
            false);
    if (appHomeDir != null)
      return appHomeDir;

    // Try directory where the executable jar resides:
    appHomeDir = searchAndProceed(getExecutableDir(true),
            "ProjectForge's home directory found relative to executable jar: $APP_HOME_DIR",
            false);
    if (appHomeDir != null)
      return appHomeDir;

    // Try current directory (launch wizard, because it's the last chance to do it):
    appHomeDir = searchAndProceed(new File("."),
            "ProjectForge's home directory not found, searched for:\n  " + StringUtils.join(getSuggestedDirectories(), "\n  "),
            true);
    if (appHomeDir != null)
      return appHomeDir;

    ProjectForgeApplication.giveUpAndSystemExit("No home directory of ProjectForge found or configured, giving up :-(");
    return null; // unreachable, because SystemExit() was called before.
  }

  /**
   * <ol>
   *   <li>
   *     If the given appHomeDir is null or blank, null is returned.
   *   </li>
   *   <li>
   *     Checks if the given appHomeDir is already given. If so, the appHomeDir is returned as File object and ProjectForge
   *     should continue the start-up phase with this directory.
   *   </li>
   *   <li>
   *     If given appHomeDir doesn't exist or isn't already initialized, the setup wizard and installation is started.
   *     If aborted by the user or any failure occurs, System.exit(1) is called, otherwise the file with the new installed
   *     home directory is returned.
   *   </li>
   * </ol>
   */
  private File proceedForced(String appHomeDir, String logMessage) {
    if (StringUtils.isNotBlank(appHomeDir)) {
      return proceed(new File(appHomeDir), logMessage, true);
    }
    return null;
  }

  private File searchAndProceed(File parentDir, String logMessage, boolean forceDirectory) {
    File appHomeDir = findBaseDirAndAncestors(parentDir);
    if (appHomeDir == null) {
      appHomeDir = new File(parentDir, "ProjectForge");
    }
    return proceed(appHomeDir, logMessage, forceDirectory);
  }

  private File proceed(File appHomeDir, String logMessage, boolean forceDirectory) {
    if (appHomeDir != null) {
      if (isProjectForgeConfigured(appHomeDir)) {
        return appHomeDir;
      }
      if (!forceDirectory) {
        return null;
      }
      new EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL, EmphasizedLogSupport.Alignment.LEFT)
              .log(logMessage.replace("$APP_HOME_DIR", appHomeDir.getPath()))
              .logEnd();
      if (userAcceptsGraphicalTerminal == null) {
        String answer = null;
        if (GraphicsEnvironment.isHeadless()) {
           answer = new ConsoleTimeoutReader("Do you want to enter the setup wizard (Y/n)?", "y")
                  .ask();
        } else {
          answer = new ConsoleTimeoutReader("Do you want to enter the setup wizard? (1), 2, 3\n  (1) Graphical wizard (default)\n  (2) Console based wizard\n  (3) Abort", "1") {
            @Override
            protected boolean answerValid(String answer) {
              return StringUtils.equalsAny(answer,"1", "2", "3");
            }
          }.ask();
        }
        if (StringUtils.startsWithIgnoreCase(answer, "y") || StringUtils.equals(answer, "2")) {
          userAcceptsGraphicalTerminal = WizardMode.CONSOLE;
        } else if (StringUtils.equals(answer, "1")) {
          userAcceptsGraphicalTerminal = WizardMode.DESKTOP;
        } else {
          userAcceptsGraphicalTerminal = WizardMode.NONE;
        }
      }
      if (userAcceptsGraphicalTerminal != WizardMode.NONE) {
        try {
          SetupData setupData;
          if (userAcceptsGraphicalTerminal == WizardMode.CONSOLE) {
            setupData = LantSetupWizard.run(appHomeDir);
          } else {
            setupData = SwingSetupWizard.run(appHomeDir);
          }
          return ProjectForgeInitializer.initialize(setupData);
        } catch (Exception ex) {
          log.error("Error while initializing new ProjectForge home: " + ex.getMessage(), ex);
          ProjectForgeApplication.giveUpAndSystemExit("Error while initializing new ProjectForge home: " + CanonicalFileUtils.absolutePath(appHomeDir));
        }
      } else {
        ProjectForgeApplication.giveUpAndSystemExit("Can't start ProjectForge in specified directory '" + CanonicalFileUtils.absolutePath(appHomeDir) + "'.");
      }
    }
    return null;
  }

  public static File[] getSuggestedDirectories() {
    List<File> files = new ArrayList<>();
    checkAndAdd(files, System.getProperty("user.home"));
    checkAndAdd(files, new File(getExecutableDir(true), "ProjectForge"));
    checkAndAdd(files, new File("ProjectForge"));
    return files.toArray(new File[files.size()]);
  }

  private static void checkAndAdd(List<File> files, String path) {
    checkAndAdd(files, CanonicalFileUtils.absolute(new File(path, "ProjectForge")));
  }

  private static void checkAndAdd(List<File> files, File dir) {
    if (isProjectForgeSourceCodeRepository(dir))
      return;
    File canonicalDir = CanonicalFileUtils.absolute(dir);
    if (!files.contains(canonicalDir))
      files.add(canonicalDir);
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
      File file = new File(new URI(location));
      for (int i = 0; i < 100; i++) {// Paranoi counter for endless loops (circular file system links)
        if (file == null) {
          return null;
        }
        if (file.exists() && file.isDirectory()) {
          return file;
        }
        file = file.getParentFile();
      }
      return null;
    } catch (URISyntaxException ex) {
      log.error("Internal error while trying to get the location of ProjectForge's running code: " + ex.getMessage(), ex);
      return null;
    }
  }

  static File findBaseDirAndAncestors(File baseDir) {
    if (baseDir == null)
      return null;
    // Need absolute directory to check parent directories.
    File currentDir = CanonicalFileUtils.absolute(baseDir); // absolute needed for getting the parent file.
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

  /**
   * @param baseDir
   * @param logWarning
   * @return true, if the given baseDir exists and is a directory, but not the source code repository root dir.
   */
  static boolean checkDirectory(File baseDir, boolean logWarning) {
    if (!baseDir.exists()) {
      if (logWarning)
        log.warn("Configured base dir '" + CanonicalFileUtils.absolutePath(baseDir) + "' doesn't exist. Ignoring it.");
      return false;
    }
    if (!baseDir.isDirectory()) {
      if (logWarning)
        log.warn("Configured base dir '" + CanonicalFileUtils.absolutePath(baseDir) + "' is not a directory. Ignoring it.");
      else
        log.warn("'" + CanonicalFileUtils.absolutePath(baseDir) + "' found, but isn't a directory, ignoring...");
      return false;
    }
    // Check for ProjectForge as source code repository:
    if (isProjectForgeSourceCodeRepository(baseDir)) {
      if (logWarning) {
        log.warn("Configured base dir '" + CanonicalFileUtils.absolutePath(baseDir) + "' seems to be the source code repository and shouldn't be used. Ignoring it.");
      }
      return false;
    }
    return true;
  }

  /**
   * @return true only and only if the given dir represents the directory $HOME/ProjectPorge or $HOME/projectforge (case insensitive).
   */
  public static boolean isStandardProjectForgeUserDir(File dir) {
    File parent = dir.getParentFile();
    if (parent == null) return false;
    try {
      if (!parent.getCanonicalPath().equals(new File(System.getProperty("user.home")).getCanonicalPath())) {
        return false;
      }
    } catch (IOException ex) {
      return false;
    }
    return "projectforge".equals(dir.getName().toLowerCase());
  }

  /**
   * @return true, if the given dir is the root directory of the source code repository, false otherwise, if the given
   * dir is a sub directory of the source code repository or any other directory.
   */
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

  /**
   * @return true, if the directory exists and contains projectforge.properties.
   */
  public static boolean isProjectForgeConfigured(String dir) {
    return StringUtils.isNotBlank(dir) && isProjectForgeConfigured(new File(dir));
  }

  /**
   * @return true, if the directory exists and contains projectforge.properties.
   */
  public static boolean isProjectForgeConfigured(File dir) {
    return dir != null && dir.exists() && new File(dir, "projectforge.properties").exists();
  }
}
