package org.projectforge.start;

import java.util.TimeZone;

import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;

import de.micromata.genome.util.runtime.Log4JInitializer;

@SpringBootApplication(scanBasePackages = { "org.projectforge", "de.micromata.mgc.jpa.spring" })
@ServletComponentScan("org.projectforge.web")
public class ProjectForgeApplication
{
  private static boolean loggingInited = false;

  public static void main(String[] args)
  {
    initLogging();
    TimeZone.setDefault(DateHelper.UTC);
    new SpringApplicationBuilder()
        .sources(ProjectForgeApplication.class)
        .run(args);
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
