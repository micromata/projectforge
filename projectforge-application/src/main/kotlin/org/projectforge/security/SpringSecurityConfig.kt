package org.projectforge.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain


@Configuration
open class SpringSecurityConfig {
    @Bean
    @Throws(Exception::class)
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests(Customizer { authorize ->
                authorize
                    .anyRequest().permitAll()
            } // Allow all requests without Authentication
            )
            .csrf({ csrf -> csrf.disable() }) // CSRF ist done by PF.

        return http.build()
    }
}
