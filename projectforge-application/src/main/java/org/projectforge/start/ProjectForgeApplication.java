package org.projectforge.start;

import java.util.TimeZone;

import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = { "org.projectforge", "de.micromata.mgc.jpa.spring" })
@ServletComponentScan({ "org.projectforge.web", "org.projectforge.business.teamcal.servlet" })
public class ProjectForgeApplication extends SpringBootServletInitializer
{
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
  {
    return application.sources(ProjectForgeApplication.class);
  }

  public static void main(String[] args) throws Exception
  {
    System.setProperty("user.timezone", "UTC");
    TimeZone.setDefault(DateHelper.UTC);
    SpringApplication.run(ProjectForgeApplication.class, args);
  }

}
