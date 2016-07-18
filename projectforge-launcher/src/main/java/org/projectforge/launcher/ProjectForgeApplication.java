package org.projectforge.launcher;

import java.io.IOException;
import java.util.TimeZone;

import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

import de.micromata.genome.util.runtime.LocalSettings;
import de.micromata.genome.util.runtime.LocalSettingsEnv;
import de.micromata.genome.util.runtime.Log4JInitializer;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@SpringBootApplication(
    scanBasePackages = { "org.projectforge", "de.micromata.mgc.jpa.spring", "de.micromata.mgc.springbootapp" })
@ServletComponentScan("org.projectforge.web")
public class ProjectForgeApplication
{
  private static boolean loggingInited = false;

  public static void main(String[] args)
  {
    initLogging();
    TimeZone.setDefault(DateHelper.UTC);
    LocalSettingsEnv.get();
    LocalSettings.get().logloadedFiles();

    SpringApplication.run(ProjectForgeApplication.class, args);
    boolean term = LocalSettings.get().getBooleanValue("projectforge.terminateoninput", false);
    if (term == false) {
      return;
    }
    System.out.println("Hit enter to terminate >");
    try {
      int readed = System.in.read();
      System.exit(0);
    } catch (IOException ex) {

    }

  }

  private static void initLogging()
  {
    if (loggingInited == true) {
      return;
    }
    Log4JInitializer.initializeLog4J();
    loggingInited = true;
  }

}
