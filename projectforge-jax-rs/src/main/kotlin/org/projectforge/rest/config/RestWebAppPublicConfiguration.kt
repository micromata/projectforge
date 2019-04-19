package org.projectforge.rest.config

import org.glassfish.jersey.server.ResourceConfig
import org.projectforge.rest.pub.SimpleLoginRest
import org.projectforge.rest.pub.SystemStatusRest

/**
 * This class configures all rest services available for the React client.
 */
class RestWebAppPublicConfiguration : ResourceConfig() {
    init {
        register(SystemStatusRest::class.java)
        register(SimpleLoginRest::class.java)
    }
}
