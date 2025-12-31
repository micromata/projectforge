/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.search.MyAnalysisConfigurer
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

    @Value("\${hibernate.hibernateHighlightSql}")
    private val hibernateHighlightSql = false

    @Value("\${hibernate.hibernateUseSqlComments}")
    private val hibernateUseSqlComments = false

    @Value("\${hibernate.generate_statistics}")
    private val hibernateGenerateStatistics = true

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
        //properties.put(AvailableSettings.DIALECT, hibernateDialect);
        properties[AvailableSettings.SHOW_SQL] = hibernateShowSql
        properties[AvailableSettings.FORMAT_SQL] = hibernateFormatSql
        properties[AvailableSettings.HIGHLIGHT_SQL] = hibernateHighlightSql
        properties[AvailableSettings.USE_SQL_COMMENTS] = hibernateUseSqlComments
        properties[AvailableSettings.GENERATE_STATISTICS] = hibernateGenerateStatistics
        properties[AvailableSettings.HBM2DDL_AUTO] = hibernateHbm2ddlAuto
        properties[AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS] = true
        properties[AvailableSettings.AUTOCOMMIT] = false
        properties[AvailableSettings.DEFAULT_BATCH_FETCH_SIZE] = 100

        /*
        properties["hibernate.javax.cache.uri"] = "classpath://ehcache.xml" // Works.
        properties["hibernate.cache.use_second_level_cache"] = true
        properties["hibernate.cache.use_query_cache"] = true
        properties["hibernate.cache.region.factory_class"] = JCacheRegionFactory::class.qualifiedName
        properties["hibernate.javax.cache.provider"] = org.ehcache.jsr107.EhcacheCachingProvider::class.qualifiedName
        */

        properties["hibernate.search.backend.analysis.configurer"] = MyAnalysisConfigurer::class.qualifiedName
        properties["hibernate.search.backend.directory.root"] = hibernateSearchDirectoryRoot
        factoryBean.setJpaProperties(properties)

        return factoryBean
    }
}
