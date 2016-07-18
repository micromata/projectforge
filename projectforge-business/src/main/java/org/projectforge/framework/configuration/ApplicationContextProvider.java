package org.projectforge.framework.configuration;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Holds the application context for implementation, which has no possibilities to be registered itself as spring bean..
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware
{
  private static ApplicationContext applicationContext;

  public static ApplicationContext getApplicationContext()
  {
    return applicationContext;
  }

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException
  {
    applicationContext = ctx;
  }
}
