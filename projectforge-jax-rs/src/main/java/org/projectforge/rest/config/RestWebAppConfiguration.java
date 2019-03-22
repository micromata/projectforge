package org.projectforge.rest.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.projectforge.rest.AddressRest;
import org.projectforge.rest.BookRest;
import org.projectforge.rest.LogoutRest;
import org.projectforge.rest.UserStatusRest;

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
    register(LogoutRest.class);

    register(AddressRest.class);
    register(BookRest.class);
  }
}