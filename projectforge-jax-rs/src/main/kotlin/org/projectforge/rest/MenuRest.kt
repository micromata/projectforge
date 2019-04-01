package org.projectforge.rest

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.Menu
import org.projectforge.menu.builder.FavoritesMenuCreator
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuCreatorContext
import org.projectforge.rest.core.RestHelper
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
    class Menus(val mainMenu: Menu, val favoritesMenu: Menu? = null)

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun logout(): Response {
        val mainMenu = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.getUser()))
        val favoritesMenu = favoritesMenuCreator.getDefaultFavoriteMenu()
        val menu = Menus(mainMenu, favoritesMenu)
        return RestHelper().buildResponse(menu)
    }
}