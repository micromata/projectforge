package org.projectforge.carddav

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class CardDavConfig {

    @Value("\${projectforge.carddav.basePath}")
    open lateinit var basePath: String
        protected set
}
