package org.projectforge.rest

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.core.RestHelper
import org.projectforge.rest.menu.builder.MenuCreator
import org.projectforge.rest.menu.builder.MenuCreatorContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Component
@Path("menu")
class MenuRest {
    @Autowired
    private lateinit var menuCreator: MenuCreator

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun logout(): Response {
        val menuItems = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.getUser()))
        return RestHelper.buildResponse(menuItems)
    }
}