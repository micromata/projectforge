package org.projectforge.start;

import java.util.TimeZone;

import org.projectforge.framework.time.DateHelper;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication(scanBasePackages = { "org.projectforge", "de.micromata.mgc.jpa.spring" })
//@EnableDiscoveryClient
@ServletComponentScan({ "org.projectforge.web", "org.projectforge.business.teamcal.servlet" })
public class ProjectForgeApplication
{
  public static void main(String[] args)
  {
    TimeZone.setDefault(DateHelper.UTC);
    new SpringApplicationBuilder()
        .sources(ProjectForgeApplication.class)
        .run(args);
  }

}
