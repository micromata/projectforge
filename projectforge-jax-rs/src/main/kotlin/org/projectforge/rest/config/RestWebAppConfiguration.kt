package org.projectforge.rest.config

import org.glassfish.jersey.media.multipart.MultiPartFeature
import org.glassfish.jersey.server.ResourceConfig
import org.projectforge.rest.*
import org.projectforge.rest.calendar.CalendarConfigServicesRest
import org.projectforge.rest.calendar.CalendarServicesRest
import org.projectforge.rest.calendar.TeamEventRest
import org.projectforge.rest.orga.ContractRest
import org.projectforge.rest.orga.PostausgangRest
import org.projectforge.rest.orga.PosteingangRest
import org.projectforge.rest.task.TaskRest
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.web.rest.MyExceptionMapper

/**
 * This class configures all rest services available for the React client.
 */
class RestWebAppConfiguration : ResourceConfig() {
    init {
        register(MultiPartFeature::class.java)
        register(MyExceptionMapper::class.java)
        register(LogoutRest::class.java)
        register(MenuRest::class.java)
        register(UserStatusRest::class.java)

        register(AddressRest::class.java)
        register(AddressImageServicesRest::class.java)
        register(AddressServicesRest::class.java)
        register(BookRest::class.java)
        register(BookServicesRest::class.java)
        register(CalendarConfigServicesRest::class.java)
        register(CalendarServicesRest::class.java)
        register(TaskRest::class.java)
        register(TaskServicesRest::class.java)
        register(TeamCalRest::class.java)
        register(TeamEventRest::class.java)
        register(TimesheetRest::class.java)
        register(UserRest::class.java)
        register(UserServicesRest::class.java)

        // Organization
        register(ContractRest::class.java)
        register(PostausgangRest::class.java)
        register(PosteingangRest::class.java)
    }
}
