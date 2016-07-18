package org.projectforge.launcher;

import org.projectforge.web.wicket.WicketApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

/**
 * This class is needed for deployment on an application server. It is the counterpart of the main method in
 * WicketWebApplication.
 *
 * @author Florian Blumenstein
 */
public class WarInitializer extends SpringBootServletInitializer
{

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
  {
    return application.sources(WicketApplication.class);
  }

}
