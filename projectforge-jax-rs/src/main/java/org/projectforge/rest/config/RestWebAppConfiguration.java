package org.projectforge.rest.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.rest.AddressDaoRest;
import org.projectforge.rest.AddressRest;
import org.projectforge.rest.BookRest;
import org.projectforge.rest.JsonUtils;
import org.projectforge.rest.pub.UserStatusRest;
import org.projectforge.rest.ui.LayoutRest;
import org.projectforge.web.rest.AuthenticationRest;
import org.projectforge.web.rest.TaskDaoRest;
import org.projectforge.web.rest.TimesheetDaoRest;
import org.projectforge.web.rest.TimesheetTemplatesRest;
import org.projectforge.web.rest.converter.PFUserDOTypeAdapter;
import org.projectforge.web.teamcal.rest.TeamCalDaoRest;
import org.projectforge.web.teamcal.rest.TeamEventDaoRest;

/**
 * This class configures all rest services available for the React client.
 * Created by blumenstein on 26.01.17.
 */
public class RestWebAppConfiguration extends ResourceConfig
{
  public RestWebAppConfiguration()
  {
    // Kotlin stuff:
    register(UserStatusRest.class);
    register(AddressRest.class);
    register(BookRest.class);
    register(LayoutRest.class);
  }
}