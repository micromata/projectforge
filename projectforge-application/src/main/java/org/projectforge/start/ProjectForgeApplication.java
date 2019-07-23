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
import org.projectforge.ProjectForgeApp;
import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.TimeZone;

@SpringBootApplication(
        scanBasePackages = {"org.projectforge", "de.micromata.mgc.jpa.spring"},
        // Needed for Spring 5.5, exception was:
        // java.lang.ClassCastException: org.springframework.orm.jpa.EntityManagerHolder cannot be cast to org.springframework.orm.hibernate5.SessionHolder
        exclude = HibernateJpaAutoConfiguration.class
)
@ServletComponentScan({"org.projectforge.web", "org.projectforge.business.teamcal.servlet"})
public class ProjectForgeApplication {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeApplication.class);

  private static final String ADDITIONAL_LOCATION_ARG = "--spring.config.additional-location=";

  private static final String PROPERTIES_FILENAME = "projectforge.properties";

  private static final String ENV_PROJECTFORGE_HOME = "PROJECTFORGE_HOME";

  private static final String COMMAND_LINE_VAR_HOME_DIR = "home.dir";

  private static final String[] DIR_NAMES = {"ProjectForge", "Projectforge", "projectforge"};

  private static final int CONSOLE_LENGTH = 80;

  public static void main(String[] args) {
    String javaVersion = System.getProperty("java.version");
    if (javaVersion != null && javaVersion.compareTo("1.9") >= 0) {
      logStartSeparator(5);
      log("ProjectForge doesn't support versions higher than Java 1.8!!!!", 5);
      log("", 5);
      log("Please downgrade. Sorry, we're working on newer Java versions.", 5);
      logEndSeparator(5);
    }
    String param = ProjectForgeApp.CONFIG_PARAM_BASE_DIR;
    String appHomeDir = System.getProperty(param); // Might be defined as -Dprojectforge.base.dir
    if (StringUtils.isBlank(appHomeDir)) {
      param = COMMAND_LINE_VAR_HOME_DIR;
      appHomeDir = System.getProperty(param); // Might be defined as -Dhome.dir=....
    }
    if (StringUtils.isNotBlank(appHomeDir)) {
      log.info("Trying ProjectForges base dir as given at commandline -D" + param + "=" + appHomeDir);
    } else {
      appHomeDir = System.getenv(ENV_PROJECTFORGE_HOME); // Environment variable.
      if (StringUtils.isNotBlank(appHomeDir)) {
        log.info("Trying ProjectForges base dir as given as system environment variable $" + ENV_PROJECTFORGE_HOME + ": " + appHomeDir);
        if (!checkDirectory(new File(appHomeDir), false)) {
          log.error("Directory '" + appHomeDir + "' configured as environment variable not found. Create this directory or unset the environment variable $" + ENV_PROJECTFORGE_HOME + ".");
        }
      }
    }
    File baseDir = StringUtils.isNotBlank(appHomeDir) ? new File(appHomeDir) : null;
    if (baseDir != null && !checkDirectory(baseDir, false)) {
      log.error("The configured base directory doesn't exist or isn't a directory, giving up :-(");
      giveUp();
    }
    if (baseDir == null) {
      // Try to find ProjectForge in current directory:
      baseDir = findBaseDir(new File("."));
      if (baseDir == null) {
        // No ProjectForge base directory found. Assuming current directory.
        baseDir = new File("ProjectForge");
        log.info("No previous ProjectForge installation found (searched for ./ProjectForge and $HOME/ProjectForge). Trying to create a new base directory: " + baseDir.getAbsolutePath());
        if (!baseDir.mkdir()) {
          log.error("Creation of directory '" + baseDir.getAbsolutePath() + "' failed, giving up :-(");
          giveUp();
        }
      }
    }
    log("", 1);
    log("Using ProjectForge directory: " + baseDir.getAbsolutePath(), 1);
    log("", 1);
    System.setProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR, baseDir.getAbsolutePath());
    if (!new File(baseDir, PROPERTIES_FILENAME).exists()) {
      logStartSeparator();
      log("Creating new ProjectForge installation!");
      log("");
      log(baseDir.getAbsolutePath());
      logEndSeparator();
    }
    ProjectForgeApp.ensureInitialConfigFile("initialProjectForge.properties", PROPERTIES_FILENAME);
    args = addDefaultAdditionalLocation(baseDir, args);
    System.setProperty("user.timezone", "UTC");
    TimeZone.setDefault(DateHelper.UTC);
    SpringApplication.run(ProjectForgeApplication.class, args);
  }

  private static void logStartSeparator() {
    logStartSeparator(2);
  }

  private static void logStartSeparator(int number) {
    for (int i = 0; i < number; i++) {
      log.info(StringUtils.rightPad("", CONSOLE_LENGTH, "*") + asterisks(number * 2 + 2));
    }
    log("", number);
  }

  private static void logEndSeparator() {
    logEndSeparator(2);
  }

  private static void logEndSeparator(int number) {
    log("", number);
    for (int i = 0; i < number; i++) {
      log.info(StringUtils.rightPad("", CONSOLE_LENGTH, "*") + asterisks(number * 2 + 2));
    }
  }

  private static void log(String text) {
    log(text, 2);
  }

  private static void log(String text, int number) {
    log.info(asterisks(number) + " " + StringUtils.center(text, CONSOLE_LENGTH) + " " + asterisks(number));
  }

  private static String asterisks(int number) {
    return StringUtils.rightPad("*", number, '*');
  }

  private static void giveUp() {
    log.info("You options (please refer: https://github.com/micromata/projectforge):");
    log.info("  1. Create ProjectForge as a top level directory of your home directory: `$HOME/ProjectForge`, or");
    log.info("  2. create a directory and define it as command line parameter: java -D" + COMMAND_LINE_VAR_HOME_DIR + "=yourdirectory -jar ...`, or");
    log.info("  3. cmreate a directory and define it as system environment variable `" + ENV_PROJECTFORGE_HOME + "`.");
    log.info("Hope to see You again ;-)");
    System.exit(1);
  }

  /**
   * For easier configuration, ProjectForge adds ${user.home}/ProjectForge/projectforge.properties as
   * --spring.config.additional-location.
   * <br/>
   * If the additional-location is already given in the args, nothing will done by this method.
   *
   * @param args The main args.
   * @return The main args extended by the new --spring.config.additional-location if not already given.
   */
  static String[] addDefaultAdditionalLocation(File baseDir, String[] args) {
    checkConfiguration("config", "application-default.properties");
    checkConfiguration("config", "application.properties");
    checkConfiguration(".", "application-default.properties");
    checkConfiguration(".", "application.properties");
    if (baseDir == null || !checkConfiguration(baseDir.getAbsolutePath(), PROPERTIES_FILENAME)) {
      return args;
    }
    // Add found projectforge.properties as additional arg (Spring uses this file with highest priority for configuration).
    boolean additionalLocationFound = false;
    if (args == null || args.length == 0) {
      args = new String[]{getAddtionalLocationArg(baseDir)};
    } else {
      for (String arg : args) {
        if (arg != null && arg.startsWith(ADDITIONAL_LOCATION_ARG)) {
          additionalLocationFound = true;
          break;
        }
      }
      if (!additionalLocationFound) {
        args = Arrays.copyOf(args, args.length + 1);
        args[args.length - 1] = getAddtionalLocationArg(baseDir);
      }
    }
    return args;
  }

  /**
   * $HOME is replaced by the user's home.
   *
   * @return --spring.config.additional-location=file:/$HOME/ProjectForge/projectforge.properties
   */
  static String getAddtionalLocationArg(File dir) {
    return ADDITIONAL_LOCATION_ARG + "file:" + getAddtionLocation(dir).getAbsolutePath();
  }

  /**
   * @param baseDir Used base dir to find projectforge.properties. If not given, the user's home dir will be used.
   * @return The projectforge.properties file in path baseDir or user's home dir.
   */
  static File getAddtionLocation(File baseDir) {
    return baseDir != null ? new File(baseDir, PROPERTIES_FILENAME)
            : Paths.get(System.getProperty("user.home"), "ProjectForge", PROPERTIES_FILENAME).toFile();
  }

  private static boolean checkConfiguration(String dir, String filename) {
    if (dir == null)
      return false;
    File file = new File(dir, filename);
    if (file.exists()) {
      log.info("Configuration from '" + file.getAbsolutePath() + "' will be used.");
      return true;
    }
    return false;
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

    try {
      URL locationUrl = ProjectForgeApplication.class.getProtectionDomain().getCodeSource().getLocation();
      String location = locationUrl.toExternalForm();
      if (location.startsWith("jar:")) {
        location = location.substring(4);
      } else {
        // Development code, don't put the ProjectForge working directory directly in the development folder:
        return null;
      }
      if (location.indexOf('!') > 0) {
        location = location.substring(0, location.indexOf('!'));
      }
      File jarFileDir = new File(new URI(location));
      dir = findBaseDirAndAncestors(jarFileDir);
      if (dir != null) {
        log.info("Using location relative to running jar: " + dir.getAbsolutePath());
        return dir;
      }
    } catch (URISyntaxException ex) {
      log.error("Internal error while trying to get the location of ProjectForge's running code: " + ex.getMessage(), ex);
    }

    // Search the user's home dir:
    String userHome = System.getProperty("user.home");
    return findBaseDirOnly(new File(userHome));
  }

  private static File findBaseDirAndAncestors(File baseDir) {
    File currentDir = baseDir;
    do {
      File dir = findBaseDirOnly(currentDir);
      if (dir != null) {
        return dir;
      }
      currentDir = currentDir.getParentFile();
    } while (currentDir != null);
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

  private static boolean checkDirectory(File baseDir, boolean logWarning) {
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
    return true;
  }
}
