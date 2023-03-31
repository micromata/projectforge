package org.projectforge.rest.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class BirthdayListConfiguration {
    @Value("\${projectforge.birthdaylist.organization}")
    open lateinit var organization: String

    @Value("\${projectforge.birthdaylist.emailAddresses}")
    open var emailAddresses: String? = null
}