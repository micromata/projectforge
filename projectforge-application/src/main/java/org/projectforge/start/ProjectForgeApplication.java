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

import org.projectforge.ProjectForgeApp;
import org.projectforge.common.EmphasizedLogSupport;
import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.ConnectorStartFailedException;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.io.File;
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

  public static final String PROPERTIES_FILENAME = "projectforge.properties";

  public static final String CLASSPATH_INITIAL_PROPERTIES_FILENAME ="initialProjectForge.properties";

  private static final String[] DIR_NAMES = {"ProjectForge", "Projectforge", "projectforge"};

  public static void main(String[] args) {
    String javaVersion = System.getProperty("java.version");
    if (javaVersion != null && javaVersion.compareTo("1.9") >= 0) {
      new EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.VERY_IMPORTANT)
              .log("ProjectForge doesn't support versions higher than Java 1.8!!!!")
              .log("")
              .log("Please downgrade. Sorry, we're working on newer Java versions.")
              .logEnd();
    }
    // Find application home or start the setup wizard, if not found:
    File baseDir = new ProjectForgeHomeFinder().findAndEnsureAppHomeDir();

    System.setProperty(ProjectForgeApp.CONFIG_PARAM_BASE_DIR, baseDir.getAbsolutePath());

    new EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL)
            .log("Using ProjectForge directory: " + baseDir.getAbsolutePath())
            .logEnd();

    args = addDefaultAdditionalLocation(baseDir, args);
    System.setProperty("user.timezone", "UTC");
    TimeZone.setDefault(DateHelper.UTC);
    try {
      SpringApplication.run(ProjectForgeApplication.class, args);
    } catch (Exception ex) {
      log.error("Exception while running application: " + ex.getMessage(), ex);
      EmphasizedLogSupport emphasizedLog = new EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.VERY_IMPORTANT, EmphasizedLogSupport.Alignment.LEFT)
              .setLogLevel(EmphasizedLogSupport.LogLevel.ERROR)
              .log("Error while running application:")
              .log("")
              .log(ex.getMessage());
      if (ex instanceof ConnectorStartFailedException) {
        emphasizedLog.log("")
                .log("May-be address of server port is already in use.");
      }
      emphasizedLog.logEnd();
      throw ex;
    }
  }

  /**
   * Show last message before exit.
   *
   * @return Does return nothing, System.exit() was called before.
   */
  public static File giveUpAndSystemExit() {
    new EmphasizedLogSupport(log, EmphasizedLogSupport.Alignment.LEFT)
            .log("Your options (please refer: https://github.com/micromata/projectforge):")
            .log("  1. Run ProjectForge and follow the setup wizard, or")
            .log("  2. Create ProjectForge as a top level directory of your home directory:")
            .log("     '$HOME/ProjectForge', or")
            .log("  3. create a directory named 'ProjectForge' and put the jar file somewhere in")
            .log("     it or in the same directory. ProjectForge detects the folder 'ProjectForge'")
            .log("     relative to the executed jar, or")
            .log("  4. create a directory and define it as command line parameter:")
            .log("     'java -D" + ProjectForgeHomeFinder.COMMAND_LINE_VAR_HOME_DIR + "=yourdirectory -jar ...', or")
            .log("  5. create a directory and define it as system environment variable")
            .log("     '" + ProjectForgeHomeFinder.getHomeEnvironmentVariableDefinition() + "'.")
            .log("Hope to see You again ;-)")
            .logEnd();
    System.exit(1);
    return null;
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
    return ADDITIONAL_LOCATION_ARG + "file:" + new File(dir, PROPERTIES_FILENAME).getAbsolutePath();
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
}
