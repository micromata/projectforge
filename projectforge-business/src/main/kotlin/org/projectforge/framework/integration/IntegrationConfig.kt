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

package org.projectforge.framework.integration

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Settings of one integration point (remote system). Every value may be null, so an override only has to
 * contain the values it changes. A value of 0 means no limit.
 */
open class IntegrationSettings(
    /** Max time for establishing the connection. */
    var connectTimeoutMs: Long? = null,
    /** Max time of inactivity while waiting for the response (read timeout). */
    var responseTimeoutMs: Long? = null,
    /** Max time for waiting for a free connection of the pool. */
    var connectionRequestTimeoutMs: Long? = null,
    /** Max size of a response body, if checked by the client (e.g. iCal subscriptions). */
    var maxResponseBytes: Long? = null,
    /** Max number of pooled connections (all hosts). */
    var maxConnections: Int? = null,
    /** Max number of pooled connections per host. */
    var maxConnectionsPerRoute: Int? = null,
)

/**
 * Resolved settings of an integration point, see [IntegrationConfig.timeouts]. 0 means no limit.
 */
data class IntegrationTimeouts(
    val connectTimeoutMs: Long,
    val responseTimeoutMs: Long,
    val connectionRequestTimeoutMs: Long,
    val maxResponseBytes: Long,
    val maxConnections: Int,
    val maxConnectionsPerRoute: Int,
)

/**
 * Timeouts and pool sizes of all integration points (LDAP, IdP, iCal subscriptions, SMS, gateway, ...), see the
 * Nygard stability report: a remote system without a timeout blocks the calling thread forever.
 *
 * Configuration (all optional):
 * ```
 * projectforge.integration.default.connectTimeoutMs=5000
 * projectforge.integration.default.responseTimeoutMs=30000
 * projectforge.integration.clients.ldap.responseTimeoutMs=10000
 * projectforge.integration.clients.gateway.responseTimeoutMs=120000
 * ```
 * Order of precedence: `clients.<id>`, the built-in defaults of the id ([BUILTIN]), `default`, [FALLBACK].
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.integration")
open class IntegrationConfig {
    var default = IntegrationSettings()

    var clients: MutableMap<String, IntegrationSettings> = mutableMapOf()

    open fun timeouts(id: String): IntegrationTimeouts {
        return resolve(id, default, clients[id])
    }

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    @PreDestroy
    private fun preDestroy() {
        PooledHttpClients.closeAll()
    }

    companion object {
        const val LDAP = "ldap"
        const val IDP = "idp"
        const val ICAL = "ical"
        const val SMS = "sms"
        const val GATEWAY = "gateway"

        /**
         * Used, if neither the id nor the default is configured.
         */
        val FALLBACK = IntegrationTimeouts(
            connectTimeoutMs = 5_000,
            responseTimeoutMs = 30_000,
            connectionRequestTimeoutMs = 5_000,
            maxResponseBytes = 0,
            maxConnections = 20,
            maxConnectionsPerRoute = 5,
        )

        /**
         * Built-in defaults of single integration points, overriding the general default.
         */
        val BUILTIN = mapOf(
            // Full syncs of thousands of addresses may take a while on the gateway.
            GATEWAY to IntegrationSettings(responseTimeoutMs = 120_000),
            ICAL to IntegrationSettings(maxResponseBytes = 10L * 1024 * 1024),
        )

        private var instance: IntegrationConfig? = null

        /**
         * For code outside of Spring beans (and tests without Spring context): uses the defaults, if the
         * configuration isn't initialized.
         */
        @JvmStatic
        fun get(id: String): IntegrationTimeouts {
            return instance?.timeouts(id) ?: resolve(id, null, null)
        }

        internal fun resolve(id: String, default: IntegrationSettings?, custom: IntegrationSettings?): IntegrationTimeouts {
            val builtin = BUILTIN[id]
            fun <T> pick(fallback: T, getter: (IntegrationSettings) -> T?): T {
                return custom?.let(getter) ?: builtin?.let(getter) ?: default?.let(getter) ?: fallback
            }
            return IntegrationTimeouts(
                connectTimeoutMs = pick(FALLBACK.connectTimeoutMs) { it.connectTimeoutMs },
                responseTimeoutMs = pick(FALLBACK.responseTimeoutMs) { it.responseTimeoutMs },
                connectionRequestTimeoutMs = pick(FALLBACK.connectionRequestTimeoutMs) { it.connectionRequestTimeoutMs },
                maxResponseBytes = pick(FALLBACK.maxResponseBytes) { it.maxResponseBytes },
                maxConnections = pick(FALLBACK.maxConnections) { it.maxConnections },
                maxConnectionsPerRoute = pick(FALLBACK.maxConnectionsPerRoute) { it.maxConnectionsPerRoute },
            )
        }
    }
}
