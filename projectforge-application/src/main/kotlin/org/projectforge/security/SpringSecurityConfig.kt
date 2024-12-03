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

package org.projectforge.security

import org.projectforge.framework.utils.NumberHelper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall


@Configuration
open class SpringSecurityConfig {
    @Bean
    @Throws(Exception::class)
    open fun securityFilterChain(http: HttpSecurity, firewall: HttpFirewall): SecurityFilterChain {
        http
            .authorizeHttpRequests(Customizer { authorize ->
                authorize
                    .anyRequest().permitAll()
            } // Allow all requests without Authentication
            )
            .csrf({ csrf -> csrf.disable() }) // CSRF ist done by PF.
        // Configure the firewall to allow WebDAV methods:
        http.setSharedObject(HttpFirewall::class.java, firewall)
        return http.build()
    }

    @Bean
    open fun allowWebDavMethodsFirewall(): HttpFirewall {
        val firewall = StrictHttpFirewall()
        // HTTP-Methoden für WebDAV explizit erlauben
        firewall.setAllowedHttpMethods(
            listOf(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD",
                "PROPFIND", //"PROPPATCH", "MKCOL", "COPY", "MOVE",
                //"LOCK", "UNLOCK", "REPORT"
            )
        )
        return firewall
    }

    @Bean
    @Override
    open fun userDetailsService(): UserDetailsService {
        /**
         * The password is generated randomly and is not stored in the database.
         * Users aren't supported, random password is better than default password, just in case.
         */
        val manager = InMemoryUserDetailsManager();
        manager.createUser(
            User.withUsername("user")
                .password(passwordEncoder().encode(NumberHelper.getSecureRandomAlphanumeric(20)))
                .roles("USER")
                .build()
        );
        return manager;
    }

    @Bean
    open fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder();
    }
}
