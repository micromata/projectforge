package org.projectforge.framework.persistence.database;

import javax.persistence.EntityManagerFactory;

import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class JpaEntityManagerFactoryFactoryBean implements FactoryBean<EntityManagerFactory>
{

  @Override
  public EntityManagerFactory getObject() throws Exception
  {
    return PfEmgrFactory.get().getEntityManagerFactory();
  }

  @Override
  public Class<?> getObjectType()
  {
    return EntityManagerFactory.class;
  }

  @Override
  public boolean isSingleton()
  {
    return true;
  }

}
