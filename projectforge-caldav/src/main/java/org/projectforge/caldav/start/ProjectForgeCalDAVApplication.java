package org.projectforge.caldav.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

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
