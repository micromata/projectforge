package org.projectforge.rest

import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.menu.builder.FavoritesMenuCreator
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuCreatorContext
import org.projectforge.menu.builder.MenuItemDef
import org.projectforge.rest.config.Rest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("${Rest.URL}/menu")
class MenuRest {
    class Menus(val mainMenu: Menu, val favoritesMenu: Menu, val myAccountMenu: Menu)

    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator

    @GetMapping
    fun getMenu(): Menus {
        val mainMenu = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.getUser()))
        val favoritesMenu = favoritesMenuCreator.getDefaultFavoriteMenu()
        val goClassicsMenu = MenuItemDef("GoClassics", "goreact.menu.classics")
        goClassicsMenu.url = "/wa"
        favoritesMenu.add(goClassicsMenu)

        val myAccountMenu = Menu()
        val item = MenuItem("username", ThreadLocalUserContext.getUser()?.fullname)
        myAccountMenu.add(item)
        item.add(MenuItem("SendFeedback", i18nKey = "menu.gear.feedback", url = "wa/feedback"))
        item.add(MenuItem("MyAccount", i18nKey = "menu.myAccount", url = "wa/myAccount"))
        item.add(MenuItem("MyLeaveAccount", i18nKey = "menu.vacation.leaveaccount", url = "wa/wicket/bookmarkable/org.projectforge.web.vacation.VacationViewPage"))
        item.add(MenuItem("Logout", i18nKey = "menu.logout", url = "logout", type = MenuItemTargetType.RESTCALL))
        item.subMenu?.forEach { it.postProcess() }
        return Menus(mainMenu, favoritesMenu, myAccountMenu)
    }
}
