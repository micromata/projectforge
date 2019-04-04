package org.projectforge.rest.config

import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.server.ResourceConfig
import org.projectforge.rest.*
import org.projectforge.web.rest.MyExceptionMapper

/**
 * This class configures all rest services available for the React client.
 * Created by blumenstein on 26.01.17.
 */
class RestWebAppConfiguration : ResourceConfig() {
    init {
        register(MultiPartFeature::class.java)
        register(MyExceptionMapper::class.java)
        register(LogoutRest::class.java)
        register(MenuRest::class.java)
        register(UserStatusRest::class.java)

        register(AddressRest::class.java)
        register(AddressImageRest::class.java)
        register(BookRest::class.java)
    }
}
