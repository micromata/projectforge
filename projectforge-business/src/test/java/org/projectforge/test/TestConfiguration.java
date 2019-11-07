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

package org.projectforge.test;

import de.micromata.genome.db.jpa.history.api.HistoryServiceManager;
import de.micromata.genome.db.jpa.history.entities.HistoryMasterBaseDO;
import de.micromata.genome.db.jpa.history.impl.HistoryServiceImpl;
import de.micromata.genome.db.jpa.tabattr.api.TimeableService;
import de.micromata.genome.db.jpa.tabattr.impl.TimeableServiceImpl;
import de.micromata.mgc.jpa.spring.SpringEmgrFilterBean;
import org.projectforge.continuousdb.DatabaseSupport;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.attr.impl.AttrSchemaServiceSpringBeanImpl;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.web.servlet.SMSReceiverServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@ComponentScan(value = {"org.projectforge", "de.micromata.mgc.jpa.spring"},
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "org.projectforge.framework.configuration.PFSpringConfiguration"),
                @ComponentScan.Filter(type = FilterType.ASPECTJ,
                        pattern = "org.projectforge.web.configuration.PFWebConfiguration")})
@PropertySource({"classpath:/application.properties", "classpath:/application-test.properties"})
@EnableTransactionManagement
//@EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class})
//Needed, because not only interfaces are used as injection points
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestConfiguration {

  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  @Value("${spring.datasource.username}")
  private String datasourceUsername;

  @Value("${spring.datasource.password}")
  private String datasourcePassword;

  @Value("${spring.datasource.driver-class-name}")
  private String datasourceDriver;

  @Value("${projectforge.base.dir}")
  private String applicationDir;

  @Autowired
  private SpringEmgrFilterBean springEmgrFilterBean;

  @Autowired
  private PfEmgrFactory pfEmgrFactory;

  @PostConstruct
  private void postConstruct() {
    if (DatabaseSupport.getInstance() == null) {
      DatabaseSupport.setInstance(new DatabaseSupport(HibernateUtils.getDialect()));
    }
  }

  /**
   * has to be defined, otherwise spring creates a LocalContainerEntityManagerFactoryBean, which has no correct
   * sessionFactory.getCurrentSession();.
   *
   * @return
   */
  @Primary
  @Bean
  public EntityManagerFactory entityManagerFactory()
  {
    return pfEmgrFactory.getEntityManagerFactory();
  }

  @Bean
  public EntityManager entityManager()
  {
    return entityManagerFactory().createEntityManager();
  }

  @Bean
  public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(emf);
    transactionManager.setDataSource(dataSource());
    transactionManager.setJpaDialect(new HibernateJpaDialect());
    return transactionManager;
  }

  @Bean
  public JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource());
  }

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public DataSource dataSource() {
    return DataSourceBuilder
            .create()
            .username(datasourceUsername)
            .password(datasourcePassword)
            .url(datasourceUrl)
            .driverClassName(datasourceDriver)
            .build();
  }

  @Bean
  public SMSReceiverServlet smsReceiverServlet() {
    return new SMSReceiverServlet();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean(name = "attrSchemaService")
  public AttrSchemaServiceSpringBeanImpl attrSchemaService() {
    AttrSchemaServiceSpringBeanImpl ret = AttrSchemaServiceSpringBeanImpl.get();
    ret.setApplicationDir(applicationDir);
    return ret;
  }

  @Bean
  public TimeableService timeableService() {
    return new TimeableServiceImpl();
  }

  /**
   * This is a workaround because we are using spring unit tests and not spring boot unit tests.
   * Without this, the spring context within our unit tests does not know this spring boot configuration bean.
   */
  @Bean
  public ServerProperties serverProperties() {
    return new ServerProperties();
  }

  @PostConstruct
  public void initEmgrFactory() {
    springEmgrFilterBean.registerEmgrFilter(pfEmgrFactory);
    HistoryServiceManager.get().setHistoryService(new HistoryServiceImpl() {

      @Override
      public Class<? extends HistoryMasterBaseDO<?, ?>> getHistoryMasterClass() {
        return PfHistoryMasterDO.class;
      }

    });
  }

}
