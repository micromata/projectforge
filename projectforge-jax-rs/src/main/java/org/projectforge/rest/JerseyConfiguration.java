package org.projectforge.rest;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.web.rest.AuthenticationRest;
import org.projectforge.web.rest.TaskDaoRest;
import org.projectforge.web.rest.TimesheetDaoRest;
import org.projectforge.web.rest.TimesheetTemplatesRest;
import org.projectforge.web.rest.converter.PFUserDOTypeAdapter;
import org.projectforge.web.teamcal.rest.TeamCalDaoRest;
import org.projectforge.web.teamcal.rest.TeamEventDaoRest;
import org.springframework.stereotype.Component;

/**
 * Created by blumenstein on 26.01.17.
 */
@Component
@ApplicationPath(RestPaths.REST)
public class JerseyConfiguration extends ResourceConfig
{
  public JerseyConfiguration()
  {
    register(AuthenticationRest.class);
    register(AddressDaoRest.class);
    register(TaskDaoRest.class);
    register(TeamCalDaoRest.class);
    register(TeamEventDaoRest.class);
    register(TimesheetDaoRest.class);
    register(TimesheetTemplatesRest.class);
    JsonUtils.add(PFUserDO.class, new PFUserDOTypeAdapter());
  }
}