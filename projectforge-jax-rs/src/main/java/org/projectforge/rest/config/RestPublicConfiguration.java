package org.projectforge.rest.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.projectforge.rest.PFVersionCheckRest;
import org.projectforge.rest.pub.SimpleLoginRest;
import org.projectforge.rest.pub.UserStatusRest;

/**
 * Created by blumenstein on 26.01.17.
 */
public class RestPublicConfiguration extends ResourceConfig
{
  public RestPublicConfiguration()
  {
    register(PFVersionCheckRest.class);

    // Kotlin stuff:
    register(SimpleLoginRest.class);
  }
}