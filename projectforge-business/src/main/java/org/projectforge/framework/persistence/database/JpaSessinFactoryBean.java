package org.projectforge.framework.persistence.database;

import org.hibernate.SessionFactory;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;

/**
 * Using EMGR.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class JpaSessinFactoryBean extends EntityManagerFactoryAccessor implements FactoryBean<SessionFactory>
{
  @Override
  public SessionFactory getObject() throws Exception
  {
    SessionFactory ret = PfEmgrFactory.get().getEntityManagerFactory().unwrap(SessionFactory.class);
    return ret;
  }

  @Override
  public Class<?> getObjectType()
  {
    return SessionFactory.class;
  }

  @Override
  public boolean isSingleton()
  {
    return true;
  }

}
