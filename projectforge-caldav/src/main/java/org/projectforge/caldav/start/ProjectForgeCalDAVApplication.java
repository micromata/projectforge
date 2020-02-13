package org.projectforge.caldav.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

// To start from Intellij:
// Edit configuration:
// Main-class: org.springframework.boot.loader.PropertiesLauncher
// VM options: -Dloader.main=org.projectforge.caldav.start.ProjectForgeCalDAVApplication -Dloader.path=env
// Environment-variables: LOADER_PATH=/Users/kai/ProjectForge/resources/caldav
// https://stackoverflow.com/questions/37833877/intellij-spring-boot-propertieslauncher/54815667#54815667?newreg=a20c33bbc5f3406d80b6b90098dbbddd

@SpringBootApplication(scanBasePackages = "org.projectforge.caldav")
@EnableJpaRepositories(basePackages = "org.projectforge.caldav.repo")
@EntityScan(basePackages = "org.projectforge.caldav.model")
//@EnableDiscoveryClient
public class ProjectForgeCalDAVApplication
{
  public static void main(String[] args)
  {
    SpringApplication.run(ProjectForgeCalDAVApplication.class);
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder)
  {
    return builder.build();
  }

}
