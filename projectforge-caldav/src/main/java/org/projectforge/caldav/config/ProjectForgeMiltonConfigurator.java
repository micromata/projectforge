package org.projectforge.caldav.config;

import org.projectforge.caldav.service.ProjectForgeCalendarSearchService;

import io.milton.ent.config.HttpManagerBuilderEnt;
import io.milton.servlet.DefaultMiltonConfigurator;

/**
 * Created by blumenstein on 17.05.17.
 */
public class ProjectForgeMiltonConfigurator extends DefaultMiltonConfigurator
{
  public ProjectForgeMiltonConfigurator()
  {
    super();
    if (builder instanceof HttpManagerBuilderEnt) {
      HttpManagerBuilderEnt builderEnt = (HttpManagerBuilderEnt) builder;
      builderEnt.setCalendarSearchService(new ProjectForgeCalendarSearchService(builderEnt));
    }
  }
}
