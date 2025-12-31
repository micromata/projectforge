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

package org.projectforge.jcr

import com.zaxxer.hikari.HikariDataSource
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Configuration
internal open class RepoConfig {
    @Value("\${oak.datasource.url}")
    private lateinit var dataSourceUrl: String

    @Value("\${oak.datasource.username}")
    private lateinit var dataSourceUser: String

    @Value("\${oak.datasource.password}")
    private lateinit var dataSourcePassword: String

    @Value("\${oak.datasource.driver-class-name}")
    private lateinit var dataSourceDriver: String

    @Value("\${oak.datasource.hikari.maximum-pool-size}")
    private var dataSourceMaximumPoolSize: Int = 10

    @Value("\${oak.datasource.hikari.minimum-idle}")
    private var dataSourceMinimumIdle: Int = 2

    @Value("\${oak.datasource.hikari.connection-timeout}")
    private var dataSourceConnectionTimeout: Long = 30000

    @Value("\${oak.datasource.hikari.idle-timeout}")
    private var dataSourceIdleTimeout: Long = 600000

    internal val dataSourceConfigured: Boolean
        get() = !dataSourceUrl.isBlank()

    var dataSource: DataSource? = null
        private set

    @PostConstruct
    private fun postConstruct() {
        if (!dataSourceConfigured) {
            return
        }
        log.info { "Creating oak datasource with url=$dataSourceUrl, user=$dataSourceUser and driver=$dataSourceDriver" }
        try {
            dataSource = DataSourceBuilder.create()
                .url(dataSourceUrl)
                .username(dataSourceUser)
                .password(dataSourcePassword)
                .driverClassName(dataSourceDriver)
                .type(HikariDataSource::class.java)
                .build().also {
                    it as HikariDataSource
                    it.minimumIdle = dataSourceMinimumIdle
                    it.maximumPoolSize = dataSourceMaximumPoolSize
                    it.connectionTimeout = dataSourceConnectionTimeout // in milliseconds (optional)
                    it.idleTimeout = dataSourceIdleTimeout // in milliseconds (optional)
                }
        } catch (e: Exception) {
            log.error(e) { "Error during data source creation: ${e.message}" }
        }
    }

    companion object {
        internal fun createForTests(): RepoConfig {
            return RepoConfig().also {
                it.dataSourceUrl = ""
            }
        }
    }
}
