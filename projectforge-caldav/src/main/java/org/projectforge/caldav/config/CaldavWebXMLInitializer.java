package org.projectforge.caldav.config;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CaldavWebXMLInitializer implements ServletContextInitializer
{

  @Override
  public void onStartup(ServletContext sc) throws ServletException
  {
    FilterRegistration miltonFilter = sc.addFilter("MiltonFilter",
        io.milton.servlet.MiltonFilter.class);
    miltonFilter.setInitParameter("resource.factory.class", "io.milton.http.annotated.AnnotationResourceFactory");
    //FB: Don't work in Spring Boot fat jar
    //    miltonFilter.setInitParameter("controllerPackagesToScan", "org.projectforge.caldav.controller");
    miltonFilter.setInitParameter("controllerClassNames",
        "org.projectforge.caldav.controller.ProjectforgeCaldavController, org.projectforge.caldav.controller.ProjectforgeCarddavController");
    miltonFilter.setInitParameter("enableDigestAuth", "false");
    miltonFilter.setInitParameter("milton.configurator", "org.projectforge.caldav.config.ProjectForgeMiltonConfigurator");
    miltonFilter.addMappingForUrlPatterns(null, false, "/*");
  }

}
