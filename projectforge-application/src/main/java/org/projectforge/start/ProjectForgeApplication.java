package org.projectforge.start;

import java.util.TimeZone;

import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication(scanBasePackages = { "org.projectforge", "de.micromata.mgc.jpa.spring" })
@ServletComponentScan({ "org.projectforge.web", "org.projectforge.business.teamcal.servlet" })
public class ProjectForgeApplication
{
  public static void main(String[] args) throws Exception
  {
    System.setProperty("user.timezone", "UTC");
    TimeZone.setDefault(DateHelper.UTC);
    SpringApplication.run(ProjectForgeApplication.class, args);
  }
}
