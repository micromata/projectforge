/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.test;

import jakarta.persistence.EntityManagerFactory;
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

import javax.sql.DataSource;
import java.util.UUID;

@Configuration
@ComponentScan(value = {"org.projectforge"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASPECTJ,
                        pattern = "org.projectforge.framework.configuration.PFSpringConfiguration"),
        })
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
        String randomSuffix = UUID.randomUUID().toString();
        return DataSourceBuilder
                .create()
                .username(datasourceUsername)
                .password(datasourcePassword)
                .url(datasourceUrl + "-" + randomSuffix) // To avoid conflicts with other parallel running tests.
                .driverClassName(datasourceDriver)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

  /*
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
*/

    /**
     * This is a workaround because we are using spring unit tests and not spring boot unit tests.
     * Without this, the spring context within our unit tests does not know this spring boot configuration bean.
     */
    @Bean
    public ServerProperties serverProperties() {
        return new ServerProperties();
    }
/*
  @PostConstruct
  public void initEmgrFactory() {
    springEmgrFilterBean.registerEmgrFilter(pfEmgrFactory);
    HistoryServiceManager.get().setHistoryService(new HistoryServiceImpl() {

      @Override
      public Class<? extends HistoryMasterBaseDO<?, ?>> getHistoryMasterClass() {
        return HistoryEntryDO.class;
      }

    });
  }*/

}
