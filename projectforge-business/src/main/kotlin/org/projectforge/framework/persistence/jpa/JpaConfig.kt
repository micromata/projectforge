/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.jpa

import org.hibernate.cfg.AvailableSettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import java.util.*
import javax.sql.DataSource

@Configuration
open class JpaConfig {
    @Value("\${hibernate.show_sql}")
    private val hibernateShowSql = false

    @Value("\${hibernate.format_sql}")
    private val hibernateFormatSql = false

    @Value("\${hibernate.hbm2ddl.auto}")
    private val hibernateHbm2ddlAuto: String? = null

    @Value("\${hibernate.search.directory.root}")
    private val hibernateSearchDirectoryRoot: String? = null

    @Bean
    open fun entityManagerFactory(dataSource: DataSource?): LocalContainerEntityManagerFactoryBean {
        val factoryBean = LocalContainerEntityManagerFactoryBean()
        factoryBean.dataSource = dataSource
        factoryBean.setPackagesToScan("org.projectforge")

        val vendorAdapter = HibernateJpaVendorAdapter()
        factoryBean.jpaVendorAdapter = vendorAdapter

        val properties = Properties()
        properties.setProperty("hibernate.hbm2ddl.auto", hibernateHbm2ddlAuto)
        //properties.put(AvailableSettings.DIALECT, hibernateDialect);
        properties[AvailableSettings.SHOW_SQL] = hibernateShowSql
        properties[AvailableSettings.FORMAT_SQL] = hibernateFormatSql
        properties[AvailableSettings.HBM2DDL_AUTO] = hibernateHbm2ddlAuto
        properties[AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS] = true
        properties[AvailableSettings.AUTOCOMMIT] = false

        properties["hibernate.search.backend.directory.root"] = hibernateSearchDirectoryRoot
        //properties["hibernate.search.backends.lucene.directory.type"] = "local-directory"
        //properties["hibernate.search.backend.type"] = "lucene"
        //properties.put(AvailableSettings.DATASOURCE, ds);
        factoryBean.setJpaProperties(properties)

        return factoryBean
    }

    /*
    @Bean
    open fun transactionManager(entityManagerFactory: EntityManagerFactory?): JpaTransactionManager {
        val transactionManager = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory
        return transactionManager
    }*/
}
