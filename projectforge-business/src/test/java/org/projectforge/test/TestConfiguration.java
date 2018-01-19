package org.projectforge.test;

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
import org.projectforge.web.servlet.SMSReceiverServlet;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
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

@Configuration
@ComponentScan(value = { "org.projectforge", "de.micromata.mgc.jpa.spring" },
    excludeFilters = { @ComponentScan.Filter(type = FilterType.ASPECTJ,
        pattern = "org.projectforge.framework.configuration.ProjectforgeSpringConfiguration"),
        @ComponentScan.Filter(type = FilterType.ASPECTJ,
            pattern = "org.projectforge.web.configuration.ProjectforgeWebConfiguration") })
@PropertySource("projectforgeTest.properties")
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestConfiguration
{

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

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer()
  {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public DataSource dataSource()
  {
    return DataSourceBuilder
        .create()
        .username(datasourceUsername)
        .password(datasourcePassword)
        .url(datasourceUrl)
        .driverClassName(datasourceDriver)
        .build();
  }

  @Bean
  public JdbcTemplate jdbcTemplate()
  {
    return new JdbcTemplate(dataSource());
  }

  @Bean
  public SMSReceiverServlet smsReceiverServlet()
  {
    return new SMSReceiverServlet();
  }

  @Bean
  public RestTemplate restTemplate()
  {
    return new RestTemplate();
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
    ret.setDataSource(dataSource());
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

  /**
   * This is a workaround because we are using spring unit tests and not spring boot unit tests.
   * Without this, the spring context within our unit tests does not know this spring boot configuration bean.
   */
  @Bean
  public ServerProperties serverProperties()
  {
    return new ServerProperties();
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
