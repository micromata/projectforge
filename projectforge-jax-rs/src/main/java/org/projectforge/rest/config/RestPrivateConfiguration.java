package org.projectforge.rest.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.rest.AddressDaoRest;
import org.projectforge.rest.AddressRest;
import org.projectforge.rest.JsonUtils;
import org.projectforge.rest.BookRest;
import org.projectforge.rest.ui.LayoutRest;
import org.projectforge.web.rest.AuthenticationRest;
import org.projectforge.web.rest.TaskDaoRest;
import org.projectforge.web.rest.TimesheetDaoRest;
import org.projectforge.web.rest.TimesheetTemplatesRest;
import org.projectforge.web.rest.converter.PFUserDOTypeAdapter;
import org.projectforge.web.teamcal.rest.TeamCalDaoRest;
import org.projectforge.web.teamcal.rest.TeamEventDaoRest;

/**
 * Created by blumenstein on 26.01.17.
 */
public class RestPrivateConfiguration extends ResourceConfig
{
  public RestPrivateConfiguration()
  {
    register(AuthenticationRest.class);
    register(AddressDaoRest.class);
    register(TaskDaoRest.class);
    register(TeamCalDaoRest.class);
    register(TeamEventDaoRest.class);
    register(TimesheetDaoRest.class);
    register(TimesheetTemplatesRest.class);
    JsonUtils.add(PFUserDO.class, new PFUserDOTypeAdapter());

    // Kotlin stuff:
    register(AddressRest.class);
    register(BookRest.class);
    register(LayoutRest.class);
  }
}