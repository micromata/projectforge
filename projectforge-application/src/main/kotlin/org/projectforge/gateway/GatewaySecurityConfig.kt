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

package org.projectforge.gateway

import org.projectforge.security.OAuth2UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall

@Configuration
@ConditionalOnProperty(name = ["projectforge.gateway.enabled"], havingValue = "true")
open class GatewaySecurityConfig {

    @Autowired(required = false)
    private var oAuth2UserService: OAuth2UserService? = null

    @Bean
    @Throws(Exception::class)
    open fun securityFilterChain(http: HttpSecurity, firewall: HttpFirewall): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    // CardDAV endpoints (authenticated by CardDavFilter via Basic Auth + DAV_TOKEN)
                    .requestMatchers("/carddav/**", "/.well-known/carddav").permitAll()
                    // ICS calendar export (authenticated via URL token parameters)
                    .requestMatchers("/export/ProjectForge.ics").permitAll()
                    // DataTransfer public endpoints (token-based external access)
                    .requestMatchers("/rsPublic/datatransfer/**").permitAll()
                    // Gateway sync API (authenticated via X-Gateway-Secret header)
                    .requestMatchers("/api/gateway/sync/**").permitAll()
                    // Static resources
                    .requestMatchers("/rsPublic/**", "/static/**", "/favicon.ico").permitAll()
                    // DataTransfer UI and REST (requires OAuth2 login)
                    .requestMatchers("/rs/datatransfer/**").authenticated()
                    // Block everything else
                    .anyRequest().denyAll()
            }
            .logout { logout ->
                logout.logoutSuccessUrl("/")
                logout.invalidateHttpSession(true)
                logout.clearAuthentication(true)
            }
            .csrf { csrf -> csrf.disable() }

        if (oAuth2UserService != null) {
            http.oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { userInfo ->
                    userInfo.oidcUserService(oAuth2UserService!!)
                }
                oauth2.defaultSuccessUrl("/rs/datatransfer", true)
            }
        }

        http.setSharedObject(HttpFirewall::class.java, firewall)
        return http.build()
    }

    @Bean
    open fun allowWebDavMethodsFirewall(): HttpFirewall {
        val firewall = StrictHttpFirewall()
        firewall.setAllowedHttpMethods(
            listOf(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD",
                "PROPFIND", "REPORT",
            )
        )
        return firewall
    }
}
