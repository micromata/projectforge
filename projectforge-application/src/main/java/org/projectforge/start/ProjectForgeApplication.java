package org.projectforge.start;

import org.projectforge.config.ServerConfiguration;
import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {"org.projectforge", "de.micromata.mgc.jpa.spring"})
@ServletComponentScan({"org.projectforge.web", "org.projectforge.business.teamcal.servlet"})
public class ProjectForgeApplication {
  static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProjectForgeApplication.class);

  private static final String ADDITIONAL_LOCATION_ARG = "--spring.config.additional-location=";

  public static void main(String[] args) throws Exception {
    args = addDefaultAdditionalLocation(args);
    System.setProperty("user.timezone", "UTC");
    TimeZone.setDefault(DateHelper.UTC);
    SpringApplication.run(ProjectForgeApplication.class, args);
  }

  /**
   * For easier configuration, ProjectForge adds ${user.home}/ProjectForge/projectforge.properties as
   * --spring.config.additional-location. If this files exists, also the value projectforge.base.dir to ${user.home}/ProjectForge.
   * <br/>
   * If the additional-location is already given in the args, nothing will done by this method.
   *
   * @param args The main args.
   * @return The main args extended by the new --spring.config.additional-location if not already given.
   */
  static String[] addDefaultAdditionalLocation(String[] args) {
    boolean additionalLocationFound = false;
    if (args == null || args.length == 0) {
      args = new String[]{getAddtionalLocationArg()};
    } else {
      for (String arg : args) {
        if (arg != null && arg.startsWith(ADDITIONAL_LOCATION_ARG)) {
          additionalLocationFound = true;
          break;
        }
      }
      if (!additionalLocationFound) {
        args = Arrays.copyOf(args, args.length + 1);
        args[args.length - 1] = getAddtionalLocationArg();
      }
    }
    if (!additionalLocationFound) {
      if (getAddtionLocation().exists()) {
        log.info("Using config file: " + getAddtionLocation().getAbsolutePath());
      }
    }
    return args;
  }

  /**
   * $HOME is replaced by the user's home.
   * @return --spring.config.additional-location=file:/$HOME/ProjectForge/projectforge.properties
   */
  static String getAddtionalLocationArg() {
    return ADDITIONAL_LOCATION_ARG + "file:" + getAddtionLocation().getAbsolutePath();
  }

  static File getAddtionLocation() {
    return Paths.get(System.getProperty("user.home"), "ProjectForge", "projectforge.properties").toFile();
  }
}
