package org.projectforge.rest

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
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
    internal class Menus(val mainMenu: Menu, val favoritesMenu: Menu, val myAccountMenu: Menu)

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun logout(): Response {
        val mainMenu = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.getUser()))
        val favoritesMenu = favoritesMenuCreator.getDefaultFavoriteMenu()
        val myAccountMenu = Menu()
        val item = MenuItem("username", ThreadLocalUserContext.getUser()?.fullname)
        myAccountMenu.add(item)
        item.add(MenuItem("SendFeedback", i18nKey = "menu.gear.feedback", url = "wa/feedback"))
        item.add(MenuItem("MyAccount", i18nKey = "menu.myAccount", url = "wa/myAccount"))
        item.add(MenuItem("MyLeaveAccount", i18nKey = "menu.vacation.leaveaccount", url = "wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationViewPage"))
        item.add(MenuItem("Logout", i18nKey = "menu.logout", url = "rs/logout", type = MenuItemTargetType.RESTCALL))
        item.subMenu?.forEach { it.postProcess() }
        val menu = Menus(mainMenu, favoritesMenu, myAccountMenu)
        return RestHelper().buildResponse(menu)
    }
}