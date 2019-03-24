package org.projectforge.rest.config

import org.glassfish.jersey.server.ResourceConfig
import org.projectforge.rest.AddressRest
import org.projectforge.rest.BookRest
import org.projectforge.rest.LogoutRest
import org.projectforge.rest.UserStatusRest

/**
 * This class configures all rest services available for the React client.
 * Created by blumenstein on 26.01.17.
 */
class RestWebAppConfiguration : ResourceConfig() {
    init {
        // Kotlin stuff:
        register(UserStatusRest::class.java)
        register(LogoutRest::class.java)

        register(AddressRest::class.java)
        register(BookRest::class.java)
    }

    override fun register(component: Class<*>?): ResourceConfig {
        return super.register(component)
    }

}