package org.projectforge.rest.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.projectforge.rest.PFVersionCheckRest;

/**
 * Created by blumenstein on 26.01.17.
 */
public class RestPublicConfiguration extends ResourceConfig
{
  public RestPublicConfiguration()
  {
    register(PFVersionCheckRest.class);
  }
}