package org.projectforge.framework.configuration;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.attr.impl.AttrSchemaServiceSpringBeanImpl;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.db.jpa.history.entities.HistoryMasterBaseDO;
import de.micromata.genome.db.jpa.history.impl.HistoryServiceImpl;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.db.jpa.tabattr.impl.TimeableServiceImpl;
import de.micromata.mgc.jpa.spring.SpringEmgrFilterBean;
import de.micromata.mgc.jpa.spring.factories.JpaToSessionFactorySpringBeanFactory;
import de.micromata.mgc.jpa.spring.factories.JpaToSessionSpringBeanFactory;

/**
 * Intial spring configuration for projectforge.
 *
 * @author Florian Blumenstein, Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Configuration
@EnableTransactionManagement
@EnableScheduling
//Needed, because not only interfaces are used as injection points
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ProjectforgeSpringConfiguration
{
  @Value("${projectforge.base.dir}")
  private String applicationDir;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private SpringEmgrFilterBean springEmgrFilterBean;

  @Autowired
  private PfEmgrFactory pfEmgrFactory;

  @Bean
  public FactoryBean<Session> hibernateSession()
  {
    return new JpaToSessionSpringBeanFactory();
  }

  @Bean
  public FactoryBean<SessionFactory> sessionFactory()
  {
    return new JpaToSessionFactorySpringBeanFactory()
    {

      @Override
      protected EntityManagerFactory getEntityManagerFactory()
      {
        return pfEmgrFactory.getEntityManagerFactory();
      }
    };

  }

  /**
   * has to be defined, otherwise spring creates a LocalContainerEntityManagerFactoryBean, which has no correct
   * sessionFactory.getCurrentSession();.
   *
   * @return
   */
  @Bean
  public EntityManagerFactory entityManagerFactory()
  {
    return pfEmgrFactory.getEntityManagerFactory();
  }

  @Bean
  public HibernateTransactionManager transactionManager() throws Exception
  {
    HibernateTransactionManager ret = new HibernateTransactionManager(sessionFactory().getObject());
    ret.setAutodetectDataSource(false);
    ret.setDataSource(dataSource);
    return ret;
  }

  @Bean
  public TransactionTemplate txTemplate() throws Exception
  {
    TransactionTemplate ret = new TransactionTemplate();
    ret.setTransactionManager(transactionManager());
    return ret;
  }

  @Bean
  public HibernateTemplate hibernateTemplate() throws Exception
  {
    HibernateTemplate ht = new HibernateTemplate(sessionFactory().getObject());
    if (DatabaseSupport.getInstance() == null) {
      DatabaseSupport.setInstance(new DatabaseSupport(HibernateUtils.getDialect()));
    }
    return ht;
  }

  @Bean(name = "attrSchemaService")
  public AttrSchemaServiceSpringBeanImpl attrSchemaService()
  {
    AttrSchemaServiceSpringBeanImpl ret = AttrSchemaServiceSpringBeanImpl.get();
    ret.setApplicationDir(applicationDir);
    return ret;
  }

  @Bean
  public TimeableService timeableService()
  {
    return new TimeableServiceImpl();
  }

  @PostConstruct
  public void initEmgrFactory()
  {
    springEmgrFilterBean.registerEmgrFilter(pfEmgrFactory);
    HistoryServiceManager.get().setHistoryService(new HistoryServiceImpl()
    {

      @Override
      public Class<? extends HistoryMasterBaseDO<?, ?>> getHistoryMasterClass()
      {
        return PfHistoryMasterDO.class;
      }

    });
  }

}
