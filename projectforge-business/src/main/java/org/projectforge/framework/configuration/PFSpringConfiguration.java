/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.db.jpa.history.entities.HistoryMasterBaseDO;
import de.micromata.genome.db.jpa.history.impl.HistoryServiceImpl;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.db.jpa.tabattr.impl.TimeableServiceImpl;
import de.micromata.mgc.jpa.spring.SpringEmgrFilterBean;
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
public class PFSpringConfiguration
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PFSpringConfiguration.class);

  @Value("${projectforge.base.dir}")
  private String applicationDir;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private SpringEmgrFilterBean springEmgrFilterBean;

  @Autowired
  private PfEmgrFactory pfEmgrFactory;

  @Value("${hibernate.search.default.indexBase}")
  private String hibernateIndexDir;

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder)
  {
    return builder.build();
  }

  @Bean
  public FactoryBean<Session> hibernateSession()
  {
    return new JpaToSessionSpringBeanFactory();
  }

  @Bean
  public SessionFactory sessionFactory()
  {
    return entityManagerFactory().unwrap(SessionFactory.class);
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
    HibernateTransactionManager ret = new HibernateTransactionManager(sessionFactory());
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
    HibernateTemplate ht = new HibernateTemplate(sessionFactory());
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
